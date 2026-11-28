import type { LegalDocument } from '../legalContent'
import { GOOGLE_PRIVACY_URL, LEGAL_CONTACT } from '../legalConstants'

/** 개인정보처리방침 포르투갈어 번역. 정본은 legalContent.ts 의 한국어(ko). 원문을 바꾸면 이 파일도 같이 고치고 version 을 맞춘다. */
export const PRIVACY_PT: LegalDocument = {
  version: 'v4',
  updatedAt: '2026-11-28',
  title: 'Política de Privacidade',
  intro:
    'O Ovlo (o "Serviço") adota a presente Política de Privacidade, nos termos da Lei de Proteção de Informações Pessoais da República da Coreia, para proteger os dados pessoais dos usuários e tratar com rapidez as reclamações relacionadas.',
  sections: [
    {
      heading: '1. Dados pessoais coletados',
      paragraphs: [
        'O Serviço coleta os seguintes dados no processo de cadastro e de uso.',
      ],
      bullets: [
        'Obrigatórios: e-mail, senha (armazenada apenas como hash), apelido, nome, cidade natal, universidade de vínculo, curso, grau acadêmico e ano',
        'Opcionais: foto de perfil, apresentação pessoal, data de nascimento, contato, habilidades linguísticas, experiência de estudo no exterior',
        'Ao usar o login social (Google): e-mail da conta Google, nome e identificador de usuário do Google',
        'Ao usar o login com a Apple: identificador de usuário da Apple, o e-mail informado pela Apple (um endereço de retransmissão da Apple, se você escolher Ocultar Meu E-mail) e o nome que você informar',
        'Na verificação por e-mail institucional: o endereço de e-mail institucional verificado, a data e hora da verificação e a universidade verificada',
        'Ao usar a orientação e o atendimento do escritório de relações internacionais da universidade: suas perguntas e as respostas automáticas, os comunicados citados nas respostas, suas avaliações das respostas, o texto de consentimento e a versão com os quais você concordou ao solicitar o atendimento e as mensagens trocadas na sala de atendimento, com as respectivas traduções',
        'Ao usar a compra e venda de usados: anúncios (título, descrição, preço, região, universidade, local de encontro, datas disponíveis), fotos dos itens, chats da negociação e motivos de denúncia',
        'Ao usar o calendário: eventos pessoais (título, descrição, local, horário) e solicitações para administrar o calendário de uma universidade (departamento, e-mail de contato, mensagem)',
        'Ao denunciar ou bloquear: o alvo da denúncia, com o motivo e a descrição, e os membros bloqueados',
        'Ao usar o envio de sugestões: o conteúdo da sua sugestão, o e-mail de resposta, se você o informar, a versão do aplicativo e as informações do navegador',
        'Informações geradas durante o uso do Serviço: publicações, comentários, mensagens de chat, arquivos anexados, registros de acesso (endereço IP, horário de acesso, caminho da requisição, número de membro) e informações do dispositivo e do navegador',
        'Cookies: token de atualização (httpOnly) para manter o login e a configuração do idioma da interface',
      ],
    },
    {
      heading: '2. Finalidades da coleta e do uso dos dados pessoais',
      paragraphs: [
        'Os dados pessoais coletados são usados somente para as finalidades a seguir.',
      ],
      bullets: [
        'Identificação de membros, login e confirmação de identidade (verificação por e-mail institucional)',
        'Oferta e operação das funções de comunidade, como murais, chat, compra e venda de usados e calendário',
        'Orientação automática com base nos comunicados do escritório de relações internacionais da universidade, encaminhamento ao responsável do escritório de relações internacionais e orientação sobre o uso do Serviço',
        'Tradução de comunicados, mensagens de chat e anúncios de usados, fornecimento de informações sobre universidades e intercâmbio e adaptação do idioma da interface',
        'Prevenção de uso indevido, tratamento de denúncias, resposta a incidentes de segurança e conservação dos registros de acesso',
        'Atendimento a consultas e envio de avisos',
      ],
    },
    {
      heading: '3. Funções que usam IA',
      paragraphs: [
        'As funções do Serviço a seguir geram respostas ou traduções por meio de um serviço de IA externo. Ao serviço de IA externo são enviados apenas o texto das perguntas ou mensagens e os comunicados ou a ajuda necessários para a resposta; informações de identificação, como nome, e-mail e número de membro, não são enviadas. No entanto, o que você mesmo escrever no texto é transmitido como está, por isso recomendamos não incluir dados pessoais sensíveis.',
      ],
      bullets: [
        'Orientação automática do escritório de relações internacionais: o estudante que concluiu a verificação por e-mail institucional recebe respostas com base nos comunicados aprovados pelo escritório de relações internacionais da sua universidade. As perguntas e respostas são armazenadas para verificar a qualidade das respostas e para dar continuidade a um atendimento (prazo de conservação no item 5).',
        'Encaminhamento ao atendimento do escritório de relações internacionais: o conteúdo da conversa só é encaminhado ao responsável do escritório de relações internacionais da sua universidade se você consentir em uma tela separada. O responsável vê as perguntas e respostas automáticas da conversa selecionada (até 20), o idioma utilizado, se o e-mail institucional foi verificado, o apelido e a foto de perfil exibidos na sala de atendimento e as mensagens posteriores da sala; além disso, a primeira pergunta é enviada junto no e-mail de aviso de novo atendimento. O endereço de e-mail e o nome não são encaminhados. As mensagens da sala de atendimento são traduzidas para o idioma de cada parte.',
        'Ajuda do Serviço: quando um membro conectado pergunta como usar o Serviço, a resposta é dada com base nos documentos de ajuda escritos pelo Serviço. O texto da pergunta é tratado apenas enquanto a resposta é gerada e não é armazenado.',
        'Tradução: os comunicados do escritório de relações internacionais e os anúncios de usados são traduzidos para o seu idioma.',
      ],
    },
    {
      heading: '4. Informações vistas pelo responsável do escritório de relações internacionais',
      paragraphs: [
        'Os responsáveis do escritório de relações internacionais são membros do Serviço convidados pelo escritório de relações internacionais da universidade correspondente. Eles só podem ver os atendimentos recebidos pela sua própria universidade e, se deixarem de ser responsáveis, não conseguem acessar as salas de atendimento. A lista de atendimentos baixada pelo responsável inclui o status, o horário, o idioma e a primeira pergunta de cada atendimento; não inclui o e-mail nem o nome do estudante.',
      ],
    },
    {
      heading: '5. Prazo de conservação e uso',
      paragraphs: [
        'Os dados pessoais são eliminados sem demora quando a finalidade da coleta e do uso é atingida. No entanto, os dados a seguir são conservados pelos prazos indicados.',
      ],
      bullets: [
        'Dados de membro: até a exclusão da conta. Ao excluir a conta, os dados da conta são imediatamente tornados não identificáveis e os arquivos que você enviou (foto de perfil e anexos) são excluídos. As publicações, os comentários, as mensagens de chat e os anúncios de usados não são excluídos, mas ocultados para que outros usuários não os vejam. Uma conta excluída não pode ser restaurada.',
        'Registros de verificação por e-mail institucional: expiram 1 ano após a data da verificação e são eliminados com a exclusão da conta.',
        'Perguntas, respostas e conteúdo dos atendimentos do escritório de relações internacionais: o texto é excluído 1 ano após o dia da pergunta (no caso dos atendimentos, após o dia em que o atendimento foi aberto) e é excluído imediatamente com a exclusão da conta. O horário de uso e o resultado do tratamento permanecem para fins estatísticos.',
        'Perguntas à ajuda do Serviço: não são armazenadas. Apenas o registro de contagem usado para limitar o número de usos é mantido, por no máximo um dia.',
        'Eventos pessoais do calendário e solicitações de administrador do calendário de uma universidade: são excluídos com a exclusão da conta.',
        'Registros de denúncias: 1 ano após o dia em que o tratamento foi concluído (nas denúncias de usados, após o dia do recebimento), o membro denunciante, o alvo da denúncia e o motivo são apagados, e permanecem, para fins estatísticos, apenas o tipo de denúncia, o resultado do tratamento e a data.',
        'Registros de bloqueio: até o desbloqueio ou até que qualquer uma das partes exclua a conta',
        'Registros de acesso (endereço IP, horário de acesso, caminho da requisição, número de membro): 3 meses (Lei de Proteção do Sigilo das Comunicações)',
        'Endereço IP e e-mail usados para limitar o número de logins, cadastros e buscas: excluídos automaticamente em até 10 minutos',
        'Backups do banco de dados: cópias de segurança são mantidas separadamente para a recuperação em caso de falhas, portanto as informações excluídas podem permanecer nos backups até que estes sejam eliminados sucessivamente. Os backups não são usados para nenhuma finalidade além da recuperação.',
      ],
    },
    {
      heading: '6. Operadores',
      paragraphs: [
        'O Serviço não vende nem fornece os dados pessoais dos usuários a terceiros. Para operar o Serviço, confiamos as seguintes atividades a operadores.',
      ],
      bullets: [
        'AI3 Inc. (BizRouter): intermediação das requisições de IA — orientação automática do escritório de relações internacionais, tradução de mensagens de atendimento, comunicados e anúncios de usados e respostas da ajuda do Serviço',
        'Anthropic PBC: geração de respostas e traduções de IA (tratadas por meio do BizRouter)',
        'Google LLC: autenticação do login com o Google e envio de e-mails pelo Gmail (códigos de verificação por e-mail institucional e avisos ao responsável do escritório de relações internacionais)',
        'Apple Inc.: autenticação do login com a Apple e desvinculação da Apple na exclusão da conta',
        'Cloudflare, Inc.: intermediação do acesso ao Serviço (segurança da transmissão) e encaminhamento dos e-mails de consulta',
      ],
    },
    {
      heading: '7. Transferência internacional de dados pessoais',
      paragraphs: [
        'Para executar as atividades confiadas, os dados pessoais são transferidos para o exterior conforme a seguir. Todas as transmissões ocorrem sempre que você usa a função correspondente, por meio de uma rede criptografada (HTTPS). O prazo de conservação do destinatário segue a política de privacidade e os termos de serviço de cada empresa.',
      ],
      bullets: [
        'Anthropic PBC (Estados Unidos, privacy@anthropic.com): perguntas ao escritório de relações internacionais e conversa anterior (até 20), comunicados e ajuda usados como base das respostas e texto das mensagens, comunicados e anúncios de usados a serem traduzidos — geração de respostas e traduções de IA',
        'Google LLC (Estados Unidos): dados da conta no login com o Google, e o endereço de destino e o conteúdo dos e-mails enviados (código de verificação, primeira pergunta do atendimento) — autenticação do login e envio de e-mails',
        'Apple Inc. (Estados Unidos): dados de confirmação do login com a Apple e o token usado na desvinculação ao excluir a conta — autenticação do login e desvinculação',
        'Cloudflare, Inc. (Estados Unidos): as requisições trocadas ao acessar o Serviço (incluindo o endereço IP) e os e-mails de consulta — intermediação do acesso e encaminhamento de e-mails',
        'Como recusar: se você não usar as funções de IA nem o login com o Google ou a Apple, essas informações não são transferidas. A intermediação do acesso pela Cloudflare é indispensável para usar o Serviço; se você recusá-la, não poderá usar o Serviço.',
      ],
    },
    {
      heading: '8. Direitos do usuário',
      paragraphs: [
        'Você pode, a qualquer momento, acessar, corrigir ou excluir seus dados pessoais ou solicitar a suspensão do tratamento, alterando-os diretamente na tela do seu perfil ou fazendo o pedido pelo contato abaixo. A exclusão da conta pode ser feita na tela do seu perfil e, nos atendimentos do escritório de relações internacionais, você pode solicitar o encerramento a qualquer momento na sala de atendimento.',
      ],
    },
    {
      heading: '9. Cookies e armazenamento local',
      paragraphs: [
        'Para manter o login, armazenamos um token de atualização em um cookie httpOnly, e guardamos a configuração do idioma da interface em um cookie e no armazenamento do navegador. Não usamos cookies para fins de publicidade ou rastreamento. Você pode recusar cookies nas configurações do navegador, mas o login poderá não ser mantido.',
      ],
    },
    {
      heading: '10. Links para vídeos externos e Serviços de API do YouTube',
      paragraphs: [
        'O Serviço usa os Serviços de API do YouTube para montar a lista de links de vídeos do YouTube exibida nas telas de relatório de universidade. Nesse processo, o Serviço não envia seus dados pessoais ao YouTube nem ao Google e também não coleta informações sobre você por meio do YouTube.',
        'No entanto, como as imagens de prévia dos vídeos são carregadas pelo seu navegador diretamente dos servidores de imagens do YouTube, ao abrir uma tela com lista de vídeos, seu endereço IP e as informações do seu navegador podem ser transmitidos ao Google. O tratamento de dados após você tocar em um link e ser levado ao YouTube segue a Política de Privacidade do Google.',
      ],
      links: [{ label: 'Política de Privacidade do Google', href: GOOGLE_PRIVACY_URL }],
    },
    {
      heading: '11. Medidas de segurança',
      paragraphs: [
        'As senhas são armazenadas apenas como hashes que não podem ser revertidos, os tokens de login são armazenados como hashes e as transmissões são criptografadas com HTTPS. O acesso de administradores é controlado por autenticação separada e por registros de acesso.',
        'As fotos enviadas em publicações e chats são armazenadas como estão, por isso as informações contidas na foto, como o local em que foi tirada, podem permanecer. Se você não quiser que a localização seja revelada, apague os dados de localização da foto antes de enviá-la. Nas fotos de itens de usados, o Serviço apaga essas informações ao armazená-las novamente.',
      ],
    },
    {
      heading: '12. Encarregado pela proteção de dados pessoais e contato',
      paragraphs: [
        `Encarregado pela proteção de dados pessoais: o operador do Serviço · Contato: ${LEGAL_CONTACT}`,
        'Para denúncias e orientações sobre violações de dados pessoais, você pode entrar em contato com o Centro de Denúncias de Violação de Informações Pessoais (privacy.kisa.or.kr, 118) ou com o Comitê de Mediação de Disputas sobre Informações Pessoais (kopico.go.kr, 1833-6972).',
      ],
    },
    {
      heading: '13. Alteração da política',
      paragraphs: [
        'Ao alterar esta política, avisaremos por meio de comunicado no Serviço a partir de 7 dias antes da entrada em vigor. Se os dados coletados, as finalidades ou os prazos de conservação forem alterados de forma desfavorável ao usuário, avisaremos com 30 dias de antecedência.',
      ],
    },
    {
      heading: '14. Idioma',
      paragraphs: ['Esta política é redigida em coreano. As traduções para outros idiomas são fornecidas por conveniência; em caso de divergência entre uma tradução e o original em coreano, prevalece o original em coreano.'],
    },
  ],
}
