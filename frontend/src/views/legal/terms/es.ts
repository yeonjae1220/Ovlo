import type { LegalDocument } from '../legalContent'
import { LEGAL_CONTACT_PLACEHOLDER, YOUTUBE_TERMS_URL } from '../legalConstants'

/** 이용약관 스페인어 번역. 정본은 legalContent.ts 의 한국어(ko). 원문을 바꾸면 이 파일도 같이 고치고 version 을 맞춘다. */
export const TERMS_ES: LegalDocument = {
  version: 'v3',
  updatedAt: '2026-11-25',
  title: 'Términos del servicio',
  intro:
    'Estos términos establecen las condiciones y los procedimientos de uso de Ovlo (en adelante, el "Servicio"), así como los derechos y obligaciones de los usuarios y del Servicio.',
  sections: [
    {
      heading: '1. Contenido del Servicio',
      paragraphs: [
        'El Servicio ofrece información sobre universidades para estudiantes de intercambio e internacionales, funciones de comunidad (tablones y chat), verificación del correo electrónico de la universidad y, en preparación, una función de orientación y derivación a consultas basada en materiales aprobados por las oficinas internacionales de las universidades.',
      ],
    },
    {
      heading: '2. Cuenta',
      paragraphs: [
        'Debes registrarte con información exacta y eres responsable de gestionar tu cuenta y tu contraseña. No puedes registrarte con la información de otra persona ni transferir o prestar tu cuenta. La verificación del correo electrónico de la universidad solo confirma que tienes acceso a esa dirección de correo; no garantiza tu condición de estudiante matriculado ni la exactitud de lo que publiques.',
      ],
    },
    {
      heading: '3. Naturaleza de la información y límites de responsabilidad',
      paragraphs: [
        'La información que ofrece el Servicio, como los informes universitarios, es material de referencia elaborado con IA a partir de fuentes públicas y materiales oficiales de las universidades, y se muestra junto con sus fuentes y su fecha de referencia. Los asuntos importantes, como visados, cuestiones académicas o importes, deben confirmarse siempre con la información oficial de la institución correspondiente; el Servicio no garantiza la exactitud ni la actualidad de la información.',
        'La función de orientación de la oficina internacional solo responde dentro del ámbito de los materiales aprobados por la universidad correspondiente y no emite juicios sobre situaciones personales (como la posibilidad de obtener un permiso de residencia). La orientación no constituye una decisión oficial de la universidad ni asesoramiento jurídico.',
      ],
    },
    {
      heading: '4. Contenido de los usuarios',
      paragraphs: [
        'Los derechos sobre las publicaciones, comentarios, mensajes y archivos que subas te pertenecen. Autorizas al Servicio a utilizarlos en la medida necesaria para mostrarlos, almacenarlos, transmitirlos y traducirlos dentro del Servicio. No debes subir obras protegidas ni datos personales de terceros sin su consentimiento.',
      ],
    },
    {
      heading: '5. Conductas prohibidas',
      paragraphs: [
        'Las siguientes conductas están prohibidas y su incumplimiento puede dar lugar a la eliminación del contenido, la restricción del uso o la baja de la cuenta.',
      ],
      bullets: [
        'Suplantar a otra persona, intentar la verificación con información falsa o compartir cuentas',
        'Publicar insultos, contenido de odio, discriminación o acoso, material obsceno o información ilegal',
        'Publicar de forma reiterada publicidad, spam o contenido con fines comerciales (salvo los avisos de colaboración que muestra el Servicio)',
        'Eludir la seguridad del Servicio o recopilar datos de forma masiva por medios automatizados',
        'Utilizar la función de orientación y consulta de la oficina internacional para denuncias falsas, bromas o amenazas',
      ],
    },
    {
      heading: '6. Tolerancia cero con el contenido ofensivo y los usuarios abusivos',
      paragraphs: [
        'El Servicio no tolera en absoluto el contenido ofensivo o inapropiado ni a los usuarios que acosan a otros. Debes aceptar este principio al registrarte, y se aplica a todo el contenido que publiquen los miembros, incluidas publicaciones, comentarios, mensajes de chat y perfiles. En particular, está prohibido el siguiente contenido:',
      ],
      bullets: [
        'Spam, publicidad, estafas o publicación reiterada del mismo contenido',
        'Acoso, intimidación, amenazas o insultos dirigidos a una persona concreta',
        'Discurso de odio o discriminación por motivos de raza, etnia, nacionalidad, religión, sexo, orientación sexual, discapacidad u otras características similares',
        'Material obsceno o contenido sexualmente explícito, y cualquier contenido que sexualice a menores',
        'Contenido que fomente o glorifique la violencia o las autolesiones',
        'Comercio de bienes o servicios ilegales y cualquier otro contenido que infrinja la ley',
        'Divulgar sin consentimiento los datos de contacto, la dirección, los documentos de identidad u otros datos personales de otras personas',
      ],
    },
    {
      heading: '7. Denuncias, bloqueos y plazo de revisión',
      paragraphs: [
        'Puedes denunciar dicho contenido o a dichos usuarios con la opción "Denunciar" de las publicaciones, los comentarios, los mensajes de chat y los perfiles de miembros, y puedes bloquear a un miembro con la opción "Bloquear". Al bloquear a alguien, dejaréis de ver las publicaciones y los comentarios del otro, no podréis enviaros mensajes y se eliminarán los seguimientos entre vosotros. Puedes desbloquear en cualquier momento desde "Miembros bloqueados" en tu perfil.',
        'Nuestro equipo de moderación revisa las denuncias recibidas en un plazo de 24 horas y, si se confirma una infracción, elimina el contenido y restringe el uso del usuario infractor o suspende o da de baja su cuenta. Las denuncias falsas y el uso abusivo de la función de denuncia también pueden dar lugar a restricciones de uso.',
      ],
    },
    {
      heading: '8. Avisos de colaboración',
      paragraphs: [
        'El Servicio puede presentar servicios externos necesarios para instalarse, como tarjetas SIM, seguros, alojamiento o recogida en el aeropuerto, identificados como "Colaboración", y puede recibir una comisión por su uso. Esta etiqueta se muestra siempre y nunca se incluye en el texto de la orientación aprobada por la oficina internacional. Los contratos y la responsabilidad de los servicios externos corresponden a sus respectivos proveedores.',
      ],
    },
    {
      heading: '9. Enlaces a vídeos externos (YouTube)',
      paragraphs: [
        'En las páginas de informes universitarios, el Servicio muestra enlaces a vídeos públicos de YouTube relacionados con esa universidad, junto con su título, el nombre del canal y una imagen de vista previa. Los vídeos no se almacenan ni se reproducen dentro del Servicio; al pulsar un enlace se te redirige a YouTube. Esta función utiliza los Servicios de API de YouTube, y al utilizarla se entiende que también aceptas los Términos del Servicio de YouTube.',
        `El contenido de cada vídeo y los derechos sobre él pertenecen a quien lo publicó, y el Servicio no garantiza su contenido. Los enlaces a vídeos eliminados o que hayan pasado a ser privados se revisan periódicamente y se retiran. Si quien publicó un vídeo o una persona que aparece en él solicita que se retire un enlace, lo retiramos sin demora. Solicitudes: ${LEGAL_CONTACT_PLACEHOLDER}`,
      ],
      links: [{ label: 'Términos del Servicio de YouTube', href: YOUTUBE_TERMS_URL }],
    },
    {
      heading: '10. Modificación e interrupción del Servicio',
      paragraphs: [
        'El Servicio puede añadir, modificar o suspender funciones, y anunciará con antelación los cambios importantes. El Servicio puede interrumpirse temporalmente por causas inevitables, como desastres naturales o fallos del sistema.',
      ],
    },
    {
      heading: '11. Resolución del contrato',
      paragraphs: [
        'Puedes darte de baja en cualquier momento desde la pantalla de tu perfil. Al darte de baja, tus datos personales se tratarán conforme a la Política de privacidad.',
      ],
    },
    {
      heading: '12. Legislación aplicable y controversias',
      paragraphs: [
        'Estos términos se rigen por las leyes de la República de Corea, y las controversias se someterán al tribunal competente conforme a la Ley de Enjuiciamiento Civil de Corea.',
      ],
    },
    {
      heading: '13. Modificación de los términos',
      paragraphs: [
        `Cuando modifiquemos estos términos, lo anunciaremos 7 días antes de su entrada en vigor (30 días antes si el cambio es desfavorable para los usuarios). Contacto: ${LEGAL_CONTACT_PLACEHOLDER}`,
      ],
    },
  ],
}
