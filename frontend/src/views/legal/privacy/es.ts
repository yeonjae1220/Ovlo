import type { LegalDocument } from '../legalContent'
import { GOOGLE_PRIVACY_URL, LEGAL_CONTACT_PLACEHOLDER } from '../legalConstants'

/** 개인정보처리방침 스페인어 번역. 정본은 legalContent.ts 의 한국어(ko). 원문을 바꾸면 이 파일도 같이 고치고 version 을 맞춘다. */
export const PRIVACY_ES: LegalDocument = {
  version: 'v3',
  updatedAt: '2026-11-25',
  title: 'Política de privacidad',
  intro:
    'Ovlo (en adelante, el "Servicio") establece la siguiente Política de privacidad, conforme a la Ley de Protección de la Información Personal de la República de Corea, para proteger los datos personales de los usuarios y atender con prontitud las reclamaciones relacionadas.',
  sections: [
    {
      heading: '1. Datos personales que recopilamos',
      paragraphs: ['El Servicio recopila los siguientes datos durante el registro y el uso del Servicio.'],
      bullets: [
        'Obligatorios: correo electrónico, contraseña (almacenada únicamente como hash), apodo, nombre, lugar de origen, universidad de pertenencia, especialidad, programa de estudios y curso',
        'Opcionales: foto de perfil, presentación personal, fecha de nacimiento, datos de contacto, competencias lingüísticas, experiencia de estudios en el extranjero',
        'Si inicias sesión con una cuenta social (Google): correo electrónico de la cuenta de Google, nombre e identificador de usuario de Google',
        'Si verificas el correo electrónico de la universidad: la dirección de correo de la universidad verificada, la fecha y hora de la verificación y la universidad verificada',
        'Información generada durante el uso del Servicio: publicaciones, comentarios, mensajes de chat y archivos adjuntos, registros de acceso (dirección IP, fecha y hora de acceso, ruta de la solicitud), información del dispositivo y del navegador',
        'Cookies: token de actualización (httpOnly) para mantener la sesión iniciada y configuración del idioma de la interfaz',
      ],
    },
    {
      heading: '2. Finalidades de la recopilación y el uso de los datos personales',
      paragraphs: ['Los datos personales recopilados se utilizan únicamente para las siguientes finalidades.'],
      bullets: [
        'Identificación de miembros, inicio de sesión y verificación de identidad (verificación del correo electrónico de la universidad)',
        'Prestación y gestión de funciones de comunidad, como tablones y chat',
        'Suministro de información sobre universidades e intercambios y adaptación del idioma de la interfaz',
        'Prevención del uso fraudulento, respuesta a incidentes de seguridad y conservación de registros de acceso',
        'Atención de consultas y envío de avisos',
      ],
    },
    {
      heading: '3. Función de orientación y consulta de la oficina internacional de la universidad (en preparación)',
      paragraphs: [
        'El Servicio está preparando una función que orienta a los estudiantes internacionales a partir de materiales aprobados por la oficina internacional de la universidad y que, solo si el usuario lo consiente, transmite el contenido de su consulta al personal responsable de la oficina internacional de esa universidad.',
        'Cuando se habilite esta función: (1) las preguntas que introduzca el usuario y las respuestas automáticas se registrarán para medir la calidad del Servicio; (2) para responder a la pregunta, el texto de la pregunta podrá encargarse para su tratamiento a un proveedor externo de servicios de IA (no se envían datos identificativos como el nombre o el correo electrónico); y (3) el contenido y el alcance de lo que se transmita al personal responsable se explicarán en una pantalla de consentimiento independiente en el momento de la transmisión. Antes de habilitar la función, actualizaremos esta política para indicar los encargados del tratamiento concretos y los plazos de conservación.',
      ],
    },
    {
      heading: '4. Plazos de conservación y uso',
      paragraphs: [
        'Los datos personales se destruyen sin demora una vez cumplida la finalidad de su recopilación y uso. No obstante, los siguientes datos se conservan durante los plazos indicados.',
      ],
      bullets: [
        'Información del miembro: hasta la baja. En el momento de la baja, los datos de la cuenta se eliminan o se anonimizan para que no permitan la identificación, y las publicaciones y los comentarios que hayas escrito dejan de ser públicos.',
        'Registros de acceso (IP, fecha y hora de acceso): 3 meses (Ley de Protección del Secreto de las Comunicaciones)',
        'Registros de verificación del correo electrónico de la universidad: caducan al cumplirse 1 año desde la fecha de verificación y se eliminan en el momento de la baja.',
        'Registros de uso fraudulento: 1 año, para evitar que se repita',
      ],
    },
    {
      heading: '5. Cesión a terceros y encargo del tratamiento',
      paragraphs: [
        'El Servicio no vende ni cede los datos personales de los usuarios a terceros. Para la gestión del Servicio, encarga las siguientes tareas.',
      ],
      bullets: [
        'Google LLC: autenticación del inicio de sesión social (Google OAuth)',
        'Proveedor de envío de correo electrónico: envío de los códigos de verificación del correo electrónico de la universidad',
        'Proveedor externo de servicios de IA: tratamiento del texto de las preguntas cuando se habilite la función de orientación de la oficina internacional (apartado 3). El encargado del tratamiento se indicará en esta política antes del inicio de la función.',
      ],
    },
    {
      heading: '6. Derechos de los usuarios',
      paragraphs: [
        'Puedes, en cualquier momento, acceder a tus datos personales, rectificarlos o eliminarlos, o solicitar la suspensión de su tratamiento, ya sea modificándolos directamente en la pantalla de tu perfil o solicitándolo a través del contacto indicado más abajo. Puedes darte de baja desde la pantalla de tu perfil.',
      ],
    },
    {
      heading: '7. Cookies y almacenamiento local',
      paragraphs: [
        'Para mantener la sesión iniciada, almacenamos un token de actualización en una cookie httpOnly, y guardamos la configuración del idioma de la interfaz en una cookie y en el almacenamiento del navegador. No utilizamos cookies con fines publicitarios ni de seguimiento. Puedes rechazar las cookies en la configuración de tu navegador, pero es posible que entonces no se mantenga la sesión iniciada.',
      ],
    },
    {
      heading: '8. Enlaces a vídeos externos y Servicios de API de YouTube',
      paragraphs: [
        'El Servicio utiliza los Servicios de API de YouTube para elaborar la lista de enlaces a vídeos de YouTube que se muestra en las páginas de informes universitarios. En este proceso, el Servicio no envía los datos personales de los usuarios a YouTube ni a Google, ni recopila información de los usuarios a través de YouTube.',
        'No obstante, como el navegador del usuario carga las imágenes de vista previa de los vídeos directamente desde los servidores de imágenes de YouTube, al abrir una página que contenga una lista de vídeos pueden transmitirse a Google la dirección IP de acceso y la información del navegador. El tratamiento de la información una vez que pulsas un enlace y accedes a YouTube se rige por la Política de Privacidad de Google.',
      ],
      links: [{ label: 'Política de Privacidad de Google', href: GOOGLE_PRIVACY_URL }],
    },
    {
      heading: '9. Medidas de seguridad',
      paragraphs: [
        'Las contraseñas se almacenan únicamente como hashes que no pueden descifrarse, los tokens de inicio de sesión se almacenan como hashes y las transmisiones se cifran mediante HTTPS. El acceso de administración se controla mediante una autenticación independiente y el registro de los accesos.',
      ],
    },
    {
      heading: '10. Responsable de la protección de datos personales y contacto',
      paragraphs: [
        `Responsable de la protección de datos personales: el operador del Servicio · Contacto: ${LEGAL_CONTACT_PLACEHOLDER}`,
        'Para denuncias y consultas sobre vulneraciones de datos personales, puedes dirigirte al Centro de Denuncias de Vulneraciones de Información Personal (privacy.kisa.or.kr, 118) o al Comité de Mediación de Controversias sobre Información Personal (kopico.go.kr, 1833-6972).',
      ],
    },
    {
      heading: '11. Modificación de la política',
      paragraphs: [
        'Cuando modifiquemos esta política, lo anunciaremos en el Servicio a partir de 7 días antes de su entrada en vigor. Si los datos recopilados, las finalidades o los plazos de conservación cambian de forma desfavorable para los usuarios, lo anunciaremos con 30 días de antelación.',
      ],
    },
  ],
}
