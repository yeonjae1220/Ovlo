package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.domain.chat.model.ChatRoomId;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;

/** The local H2 profile must retain the same concurrent read-marker behavior as production PostgreSQL. */
class ChatReadMarkerPersistenceTest {
    @Test void concurrentInitialReadsAlsoWorkInTheLocalDatabase() throws Exception {
        var ds=new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=PostgreSQL;DB_CLOSE_DELAY=-1","sa","");
        var db=new JdbcTemplate(ds);var tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        db.execute("CREATE TABLE chat_room(id BIGINT PRIMARY KEY)");
        db.execute("CREATE TABLE chat_room_read_marker(chat_room_id BIGINT REFERENCES chat_room(id),member_id BIGINT,last_read_at TIMESTAMP WITH TIME ZONE NOT NULL,PRIMARY KEY(chat_room_id,member_id))");
        db.update("INSERT INTO chat_room VALUES(1)");
        var adapter=new ChatPersistenceAdapter(null,null,null,null,null,db);
        var start=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(8)) {
            var futures=new ArrayList<Future<?>>();
            for(int i=0;i<8;i++)futures.add(pool.submit(()->{start.await();tx.executeWithoutResult(s->adapter.markRead(new ChatRoomId(1L),new MemberId(1L)));return null;}));
            start.countDown();for(var future:futures)future.get(10,TimeUnit.SECONDS);
            assertThat(db.queryForObject("SELECT count(*) FROM chat_room_read_marker",Long.class)).isEqualTo(1);
        } finally { db.execute("SHUTDOWN"); }
    }
}
