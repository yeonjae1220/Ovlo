import type { LegalDocument } from '../legalContent'
import { GOOGLE_PRIVACY_URL, LEGAL_CONTACT } from '../legalConstants'

/** 개인정보처리방침 포르투갈어 번역. 정본은 legalContent.ts 의 한국어(ko). 원문을 바꾸면 이 파일도 같이 고치고 version 을 맞춘다. */
export const PRIVACY_PT: LegalDocument = {
  version: 'v3',
  updatedAt: '2026-11-25',
  title: 'Política de Privacidade',
  intro:
    'O Ovlo (o "Serviço") adota a presente Política de Privacidade, nos termos da Lei de Proteção de Informações Pessoais da República da Coreia, para proteger os dados pessoais dos usuários e tratar com rapidez as reclamações relacionadas.',
  sections: [
    {
      heading: '1. Dados pessoais coletados',
      paragraphs: ['O Serviço coleta os seguintes dados no processo de cadastro e de uso.'],
      bullets: [
        'Obrigatórios: e-mail, senha (armazenada apenas como hash), apelido, nome, cidade natal, universidade de vínculo, curso, grau acadêmico e ano',
        'Opcionais: foto de perfil, apresentação pessoal, data de nascimento, contato, habilidades linguísticas, experiência de estudo no exterior',
        'Ao usar o login social (Google): e-mail da conta Google, nome e identificador de usuário do Google',
        'Na verificação por e-mail institucional: o endereço de e-mail institucional verificado, a data e hora da verificação e a universidade verificada',
        'Informações geradas durante o uso do Serviço: publicações, comentários, mensagens de chat, arquivos anexados, registros de acesso (endereço IP, horário de acesso, caminho da requisição) e informações do dispositivo e do navegador',
        'Cookies: token de atualização (httpOnly) para manter o login e a configuração do idioma da interface',
      ],
    },
    {
      heading: '2. Finalidades da coleta e do uso dos dados pessoais',
      paragraphs: ['Os dados pessoais coletados são usados somente para as finalidades a seguir.'],
      bullets: [
        'Identificação de membros, login e confirmação de identidade (verificação por e-mail institucional)',
        'Oferta e operação das funções de comunidade, como murais e chat',
        'Fornecimento de informações sobre universidades e intercâmbio, e adaptação do idioma da interface',
        'Prevenção de uso indevido, resposta a incidentes de segurança e conservação dos registros de acesso',
        'Atendimento a consultas e envio de avisos',
      ],
    },
    {
      heading: '3. Função de orientação e atendimento do escritório de relações internacionais da universidade (em preparação)',
      paragraphs: [
        'O Serviço está preparando uma função que orienta estudantes internacionais com base em materiais aprovados pelo escritório de relações internacionais da universidade e que, somente com o seu consentimento, encaminha o conteúdo da sua consulta ao responsável no escritório de relações internacionais da respectiva universidade.',
        'Quando essa função for lançada, (1) as perguntas que você inserir e as respostas automáticas serão registradas para medir a qualidade do Serviço; (2) o texto da pergunta poderá ser confiado, para tratamento, a um fornecedor externo de serviços de IA a fim de responder à pergunta (informações de identificação, como nome e e-mail, não são enviadas); e (3) o conteúdo e o alcance do que for encaminhado ao responsável serão informados em uma tela de consentimento separada no momento do encaminhamento. Antes do lançamento da função, atualizaremos esta política para indicar os operadores específicos e os prazos de conservação.',
      ],
    },
    {
      heading: '4. Prazo de conservação e uso',
      paragraphs: [
        'Os dados pessoais são eliminados sem demora quando a finalidade da coleta e do uso é atingida. No entanto, os dados a seguir são conservados pelos prazos indicados.',
      ],
      bullets: [
        'Dados de membro: até a exclusão da conta. Imediatamente após a exclusão da conta, os dados da conta são eliminados ou tornados não identificáveis, e as publicações e os comentários que você escreveu deixam de ser públicos.',
        'Registros de acesso (IP, horário de acesso): 3 meses (Lei de Proteção do Sigilo das Comunicações)',
        'Registros de verificação por e-mail institucional: expiram 1 ano após a data da verificação e são eliminados com a exclusão da conta.',
        'Registros de uso indevido: 1 ano, para evitar a reincidência',
      ],
    },
    {
      heading: '5. Fornecimento a terceiros e operadores',
      paragraphs: [
        'O Serviço não vende nem fornece os dados pessoais dos usuários a terceiros. Para operar o Serviço, confiamos as seguintes atividades a operadores.',
      ],
      bullets: [
        'Google LLC: autenticação do login social (Google OAuth)',
        'Fornecedor de envio de e-mails: envio dos códigos de verificação por e-mail institucional',
        'Fornecedor externo de serviços de IA: tratamento do texto das perguntas quando a função de orientação do escritório de relações internacionais (item 3) for lançada. O operador será indicado nesta política antes do lançamento da função.',
      ],
    },
    {
      heading: '6. Direitos do usuário',
      paragraphs: [
        'Você pode, a qualquer momento, acessar, corrigir ou excluir seus dados pessoais ou solicitar a suspensão do tratamento, alterando-os diretamente na tela do seu perfil ou fazendo o pedido pelo contato abaixo. A exclusão da conta pode ser feita na tela do seu perfil.',
      ],
    },
    {
      heading: '7. Cookies e armazenamento local',
      paragraphs: [
        'Para manter o login, armazenamos um token de atualização em um cookie httpOnly, e guardamos a configuração do idioma da interface em um cookie e no armazenamento do navegador. Não usamos cookies para fins de publicidade ou rastreamento. Você pode recusar cookies nas configurações do navegador, mas o login poderá não ser mantido.',
      ],
    },
    {
      heading: '8. Links para vídeos externos e Serviços de API do YouTube',
      paragraphs: [
        'O Serviço usa os Serviços de API do YouTube para montar a lista de links de vídeos do YouTube exibida nas telas de relatório de universidade. Nesse processo, o Serviço não envia seus dados pessoais ao YouTube nem ao Google e também não coleta informações sobre você por meio do YouTube.',
        'No entanto, como as imagens de prévia dos vídeos são carregadas pelo seu navegador diretamente dos servidores de imagens do YouTube, ao abrir uma tela com lista de vídeos, seu endereço IP e as informações do seu navegador podem ser transmitidos ao Google. O tratamento de dados após você tocar em um link e ser levado ao YouTube segue a Política de Privacidade do Google.',
      ],
      links: [{ label: 'Política de Privacidade do Google', href: GOOGLE_PRIVACY_URL }],
    },
    {
      heading: '9. Medidas de segurança',
      paragraphs: [
        'As senhas são armazenadas apenas como hashes que não podem ser revertidos, os tokens de login são armazenados como hashes e as transmissões são criptografadas com HTTPS. O acesso de administradores é controlado por autenticação separada e por registros de acesso.',
      ],
    },
    {
      heading: '10. Encarregado pela proteção de dados pessoais e contato',
      paragraphs: [
        `Encarregado pela proteção de dados pessoais: o operador do Serviço · Contato: ${LEGAL_CONTACT}`,
        'Para denúncias e orientações sobre violações de dados pessoais, você pode entrar em contato com o Centro de Denúncias de Violação de Informações Pessoais (privacy.kisa.or.kr, 118) ou com o Comitê de Mediação de Disputas sobre Informações Pessoais (kopico.go.kr, 1833-6972).',
      ],
    },
    {
      heading: '11. Alteração da política',
      paragraphs: [
        'Ao alterar esta política, avisaremos por meio de comunicado no Serviço a partir de 7 dias antes da entrada em vigor. Se os dados coletados, as finalidades ou os prazos de conservação forem alterados de forma desfavorável ao usuário, avisaremos com 30 dias de antecedência.',
      ],
    },
    {
      heading: '12. Idioma',
      paragraphs: ['Esta política é redigida em coreano. As traduções para outros idiomas são fornecidas por conveniência; em caso de divergência entre uma tradução e o original em coreano, prevalece o original em coreano.'],
    },
  ],
}
