import type { LegalDocument } from '../legalContent'
import { LEGAL_CONTACT_PLACEHOLDER, YOUTUBE_TERMS_URL } from '../legalConstants'

/** 이용약관 포르투갈어 번역. 정본은 legalContent.ts 의 한국어(ko). 원문을 바꾸면 이 파일도 같이 고치고 version 을 맞춘다. */
export const TERMS_PT: LegalDocument = {
  version: 'v3',
  updatedAt: '2026-11-25',
  title: 'Termos de Uso',
  intro:
    'Estes termos estabelecem as condições e os procedimentos de uso do Ovlo (o "Serviço"), bem como os direitos e as obrigações dos usuários e do Serviço.',
  sections: [
    {
      heading: '1. Conteúdo do Serviço',
      paragraphs: [
        'O Serviço oferece informações sobre universidades para estudantes de intercâmbio e estudantes internacionais, uma comunidade (murais e chat), a verificação por e-mail institucional e, em preparação, uma função de orientação e de encaminhamento para atendimento baseada em materiais aprovados pelos escritórios de relações internacionais das universidades.',
      ],
    },
    {
      heading: '2. Conta',
      paragraphs: [
        'Você deve se cadastrar com informações corretas e é responsável por administrar sua conta e sua senha. Não é permitido se cadastrar com informações de outra pessoa nem transferir ou emprestar sua conta. A verificação por e-mail institucional confirma apenas que você tem acesso a esse endereço de e-mail; ela não garante sua matrícula nem a exatidão do que você publica.',
      ],
    },
    {
      heading: '3. Natureza das informações e limites de responsabilidade',
      paragraphs: [
        'As informações fornecidas pelo Serviço, como os relatórios de universidades, são material de referência organizado com IA a partir de fontes públicas e de materiais oficiais das universidades, exibido com as fontes e a data de referência. Assuntos importantes, como visto, questões acadêmicas e valores, devem sempre ser confirmados nas orientações oficiais da instituição competente. O Serviço não garante a exatidão nem a atualidade das informações.',
        'A função de orientação do escritório de relações internacionais responde apenas dentro do escopo dos materiais aprovados pela respectiva universidade e não avalia situações pessoais (como a possibilidade de obter determinado status de permanência). A orientação não constitui decisão oficial da universidade nem aconselhamento jurídico.',
      ],
    },
    {
      heading: '4. Conteúdo publicado pelo usuário',
      paragraphs: [
        'Os direitos sobre as publicações, os comentários, as mensagens e os arquivos que você envia pertencem a você. Você autoriza o Serviço a utilizá-los na medida necessária para exibi-los, armazená-los, transmiti-los e traduzi-los dentro do Serviço. Você não deve enviar obras protegidas por direitos autorais ou dados pessoais de terceiros sem o consentimento deles.',
      ],
    },
    {
      heading: '5. Condutas proibidas',
      paragraphs: [
        'As condutas a seguir são proibidas e, em caso de violação, podem resultar na remoção do conteúdo, na restrição do uso ou no encerramento da conta.',
      ],
      bullets: [
        'Fazer-se passar por outra pessoa, tentar a verificação com informações falsas ou compartilhar contas',
        'Publicar ofensas, discurso de ódio, discriminação, assédio, conteúdo obsceno ou informações ilegais',
        'Publicidade, spam ou publicações repetidas com fins comerciais (exceto os avisos de parceiros exibidos pelo Serviço)',
        'Contornar a segurança do Serviço ou coletar dados em massa por meios automatizados',
        'Usar a função de orientação e atendimento do escritório de relações internacionais para denúncias falsas, brincadeiras ou ameaças',
      ],
    },
    {
      heading: '6. Tolerância zero com conteúdo ofensivo e usuários abusivos',
      paragraphs: [
        'O Serviço não tolera, em hipótese alguma, conteúdo ofensivo ou inadequado nem usuários que assediem outros usuários. Você deve concordar com este princípio ao se cadastrar, e ele se aplica a todo o conteúdo que os membros publicam, incluindo publicações, comentários, mensagens de chat e perfis. Em especial, o seguinte conteúdo é proibido:',
      ],
      bullets: [
        'Spam, publicidade, golpes ou publicação repetida do mesmo conteúdo',
        'Assédio, intimidação (bullying), ameaças ou insultos dirigidos a uma pessoa específica',
        'Discurso de ódio e discriminação com base em raça, etnia, nacionalidade, religião, sexo, orientação sexual, deficiência ou características semelhantes',
        'Pornografia ou conteúdo sexualmente explícito, e qualquer conteúdo que sexualize menores de idade',
        'Conteúdo que incentive ou glorifique a violência ou a automutilação',
        'Comércio de produtos ou serviços ilegais, ou qualquer outro conteúdo que viole a lei',
        'Divulgar sem consentimento dados pessoais de outras pessoas, como contatos, endereços ou documentos de identidade',
      ],
    },
    {
      heading: '7. Denúncia, bloqueio e prazo de análise',
      paragraphs: [
        'Você pode denunciar o conteúdo ou os usuários descritos acima com a função "Denunciar" disponível em publicações, comentários, mensagens de chat e perfis de membros, e pode bloquear um membro específico com a função "Bloquear". Quando há um bloqueio, vocês deixam de ver as publicações e os comentários um do outro, não podem trocar mensagens e os vínculos de seguir entre vocês são removidos. Você pode desbloquear a qualquer momento na tela "Membros bloqueados" do seu perfil.',
        'As denúncias recebidas são analisadas pela equipe de moderação em até 24 horas. Se a violação for confirmada, o conteúdo é removido e o uso do usuário infrator é restringido ou sua conta é suspensa ou encerrada. Denúncias falsas e o uso abusivo da função de denúncia também são motivo para restrição do uso.',
      ],
    },
    {
      heading: '8. Avisos de parceiros',
      paragraphs: [
        'O Serviço pode apresentar, identificados como "Parceiro", serviços externos necessários para a instalação, como chip de celular (SIM), seguro, moradia e transfer do aeroporto, e pode receber uma comissão quando você os utiliza. A identificação de parceiro é sempre exibida e nunca é incluída no texto das orientações aprovadas pelo escritório de relações internacionais. Os contratos e a responsabilidade pelos serviços externos cabem aos respectivos fornecedores.',
      ],
    },
    {
      heading: '9. Links para vídeos externos (YouTube)',
      paragraphs: [
        'Nas telas de relatório de universidade, o Serviço exibe links para vídeos públicos do YouTube relacionados a essa universidade, com título, nome do canal e imagem de prévia. Os vídeos não são armazenados nem reproduzidos dentro do Serviço; ao tocar em um link, você é levado ao YouTube. Esta função utiliza os Serviços de API do YouTube e, ao utilizá-la, você também concorda com os Termos de Serviço do YouTube.',
        `O conteúdo e os direitos de cada vídeo pertencem a quem o publicou, e o Serviço não garante o conteúdo dos vídeos. Os links para vídeos excluídos ou tornados privados são verificados periodicamente e removidos. Se quem publicou o vídeo ou uma pessoa que aparece nele pedir a remoção de um link, nós o removeremos sem demora. Pedidos: ${LEGAL_CONTACT_PLACEHOLDER}`,
      ],
      links: [{ label: 'Termos de Serviço do YouTube', href: YOUTUBE_TERMS_URL }],
    },
    {
      heading: '10. Alteração e interrupção do Serviço',
      paragraphs: [
        'O Serviço pode adicionar, alterar ou descontinuar funções, e as alterações importantes são comunicadas com antecedência. O Serviço pode ser interrompido temporariamente por motivos inevitáveis, como desastres naturais ou falhas de sistema.',
      ],
    },
    {
      heading: '11. Rescisão',
      paragraphs: [
        'Você pode excluir sua conta a qualquer momento na tela do seu perfil. Ao excluir a conta, seus dados pessoais são tratados de acordo com a Política de Privacidade.',
      ],
    },
    {
      heading: '12. Lei aplicável e disputas',
      paragraphs: [
        'Estes termos são regidos pelas leis da República da Coreia, e as disputas são julgadas pelo tribunal competente nos termos da Lei de Processo Civil.',
      ],
    },
    {
      heading: '13. Alteração dos termos',
      paragraphs: [
        `As alterações destes termos são comunicadas 7 dias antes de entrarem em vigor (30 dias antes, no caso de alterações desfavoráveis aos usuários). Contato: ${LEGAL_CONTACT_PLACEHOLDER}`,
      ],
    },
  ],
}
