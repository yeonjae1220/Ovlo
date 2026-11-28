import type { LegalDocument } from '../legalContent'
import { GOOGLE_PRIVACY_URL, LEGAL_CONTACT } from '../legalConstants'

/** 개인정보처리방침 스페인어 번역. 정본은 legalContent.ts 의 한국어(ko). 원문을 바꾸면 이 파일도 같이 고치고 version 을 맞춘다. */
export const PRIVACY_ES: LegalDocument = {
  version: 'v4',
  updatedAt: '2026-11-28',
  title: 'Política de privacidad',
  intro:
    'Ovlo (en adelante, el "Servicio") establece la siguiente Política de privacidad, conforme a la Ley de Protección de la Información Personal de la República de Corea, para proteger los datos personales de los usuarios y atender con prontitud las reclamaciones relacionadas.',
  sections: [
    {
      heading: '1. Datos personales que recopilamos',
      paragraphs: [
        'El Servicio recopila los siguientes datos durante el registro y el uso del Servicio.',
      ],
      bullets: [
        'Obligatorios: correo electrónico, contraseña (almacenada únicamente como hash), apodo, nombre, lugar de origen, universidad de pertenencia, especialidad, programa de estudios y curso',
        'Opcionales: foto de perfil, presentación personal, fecha de nacimiento, datos de contacto, competencias lingüísticas, experiencia de estudios en el extranjero',
        'Si inicias sesión con una cuenta social (Google): correo electrónico de la cuenta de Google, nombre e identificador de usuario de Google',
        'Si inicias sesión con Apple: identificador de usuario de Apple, el correo electrónico que Apple nos facilita (la dirección de retransmisión de Apple si eliges Ocultar mi correo electrónico) y el nombre que nos indiques',
        'Si verificas el correo electrónico de la universidad: la dirección de correo de la universidad verificada, la fecha y hora de la verificación y la universidad verificada',
        'Si usas la orientación y la consulta de la oficina internacional de la universidad: tus preguntas y las respuestas automáticas, los avisos citados en las respuestas, tus valoraciones de las respuestas, el texto de consentimiento y su versión aceptados al solicitar una consulta, y los mensajes intercambiados en la sala de consulta junto con sus traducciones',
        'Si usas la compraventa de segunda mano: anuncios (título, descripción, precio, zona, universidad, lugar de encuentro, fechas disponibles), fotos de los artículos, chats de la transacción y motivos de denuncia',
        'Si usas el calendario: eventos personales (título, descripción, lugar, hora) y solicitudes para gestionar el calendario de una universidad (departamento, correo electrónico de contacto, mensaje)',
        'Al denunciar o bloquear: el objeto de la denuncia, con el motivo y la descripción, y los miembros bloqueados',
        'Si usas el formulario de comentarios: el contenido de tus comentarios, el correo electrónico de respuesta si lo indicas, la versión de la aplicación y la información del navegador',
        'Información generada durante el uso del Servicio: publicaciones, comentarios, mensajes de chat y archivos adjuntos, registros de acceso (dirección IP, fecha y hora de acceso, ruta de la solicitud, número de miembro), información del dispositivo y del navegador',
        'Cookies: token de actualización (httpOnly) para mantener la sesión iniciada y configuración del idioma de la interfaz',
      ],
    },
    {
      heading: '2. Finalidades de la recopilación y el uso de los datos personales',
      paragraphs: [
        'Los datos personales recopilados se utilizan únicamente para las siguientes finalidades.',
      ],
      bullets: [
        'Identificación de miembros, inicio de sesión y verificación de identidad (verificación del correo electrónico de la universidad)',
        'Prestación y gestión de funciones de comunidad, como tablones, chat, compraventa de segunda mano y calendario',
        'Orientación automática basada en los avisos de la oficina internacional de la universidad, conexión con el personal responsable de la oficina internacional y orientación sobre el uso del Servicio',
        'Traducción de avisos, mensajes de chat y anuncios de segunda mano, suministro de información sobre universidades e intercambios y adaptación del idioma de la interfaz',
        'Prevención del uso fraudulento, tramitación de denuncias, respuesta a incidentes de seguridad y conservación de registros de acceso',
        'Atención de consultas y envío de avisos',
      ],
    },
    {
      heading: '3. Funciones que utilizan IA',
      paragraphs: [
        'Las siguientes funciones del Servicio generan respuestas o traducciones mediante un servicio de IA externo. Al servicio de IA externo solo se envían el texto de las preguntas o mensajes y los avisos o la ayuda necesarios para responder; no se envían datos identificativos como el nombre, el correo electrónico o el número de miembro. No obstante, lo que escribas tú mismo en el texto se transmite tal cual, por lo que te recomendamos no incluir datos personales sensibles.',
      ],
      bullets: [
        'Orientación automática de la oficina internacional: el estudiante que ha completado la verificación del correo electrónico de la universidad recibe respuestas basadas en los avisos aprobados por la oficina internacional de su universidad. Las preguntas y respuestas se almacenan para comprobar la calidad de las respuestas y para continuar en una consulta (plazo de conservación en el apartado 5).',
        'Conexión con la consulta de la oficina internacional: el contenido de la conversación solo se transmite al personal responsable de la oficina internacional de tu universidad si das tu consentimiento en una pantalla independiente. El personal responsable ve las preguntas y respuestas automáticas de la conversación seleccionada (hasta 20), el idioma utilizado, si el correo electrónico de la universidad está verificado, el apodo y la foto de perfil que se muestran en la sala de consulta y los mensajes posteriores de la sala; además, el primer mensaje se incluye en el correo de aviso de nueva consulta. No se transmiten la dirección de correo electrónico ni el nombre. Los mensajes de la sala de consulta se traducen al idioma de cada parte.',
        'Ayuda del Servicio: cuando un miembro con sesión iniciada pregunta cómo usar el Servicio, se responde con base en los documentos de ayuda redactados por el Servicio. El texto de la pregunta se trata solo mientras se genera la respuesta y no se almacena.',
        'Traducción: los avisos de la oficina internacional y los anuncios de segunda mano se traducen a tu idioma.',
      ],
    },
    {
      heading: '4. Información que ve el personal de la oficina internacional',
      paragraphs: [
        'El personal responsable de la oficina internacional son miembros del Servicio invitados por la oficina internacional de la universidad correspondiente. Solo pueden ver las consultas recibidas por su propia universidad y, si dejan de ser responsables, no pueden acceder a las salas de consulta. La lista de consultas que descarga el personal responsable incluye el estado, la hora, el idioma y la primera pregunta de cada consulta; no incluye el correo electrónico ni el nombre del estudiante.',
      ],
    },
    {
      heading: '5. Plazos de conservación y uso',
      paragraphs: [
        'Los datos personales se destruyen sin demora una vez cumplida la finalidad de su recopilación y uso. No obstante, los siguientes datos se conservan durante los plazos indicados.',
      ],
      bullets: [
        'Información del miembro: hasta la baja. Al darte de baja, los datos de la cuenta se anonimizan de inmediato para que no permitan la identificación y se eliminan los archivos que subiste (foto de perfil y archivos adjuntos). Las publicaciones, los comentarios, los mensajes de chat y los anuncios de segunda mano no se eliminan, sino que se ocultan para que otros usuarios no los vean. Una cuenta dada de baja no puede recuperarse.',
        'Registros de verificación del correo electrónico de la universidad: caducan al cumplirse 1 año desde la fecha de verificación y se eliminan en el momento de la baja.',
        'Preguntas, respuestas y contenido de las consultas de la oficina internacional: el texto se elimina al cumplirse 1 año desde el día de la pregunta (en las consultas, desde el día en que se abrió la consulta) y se elimina de inmediato al darte de baja. La hora de uso y el resultado del tratamiento se conservan con fines estadísticos.',
        'Preguntas a la ayuda del Servicio: no se almacenan. Solo se conserva, como máximo durante un día, el recuento de usos para limitar su número.',
        'Eventos personales del calendario y solicitudes de gestor del calendario de una universidad: se eliminan en el momento de la baja.',
        'Registros de denuncias: al cumplirse 1 año desde el día en que concluyó su tramitación (en las denuncias de segunda mano, desde el día de su recepción) se borran el miembro denunciante, el objeto de la denuncia y el motivo, y solo se conservan con fines estadísticos el tipo de denuncia, el resultado del tratamiento y la fecha.',
        'Registros de bloqueo: hasta que se desbloquee o hasta que cualquiera de las dos partes se dé de baja',
        'Registros de acceso (dirección IP, fecha y hora de acceso, ruta de la solicitud, número de miembro): 3 meses (Ley de Protección del Secreto de las Comunicaciones)',
        'Dirección IP y correo electrónico utilizados para limitar el número de inicios de sesión, registros y búsquedas: se eliminan automáticamente en un plazo de 10 minutos',
        'Copias de seguridad de la base de datos: se conservan copias de seguridad por separado para la recuperación ante fallos, por lo que la información eliminada puede permanecer en ellas hasta que se vayan eliminando de forma sucesiva. Las copias de seguridad no se utilizan con ninguna finalidad distinta de la recuperación.',
      ],
    },
    {
      heading: '6. Encargo del tratamiento',
      paragraphs: [
        'El Servicio no vende ni cede los datos personales de los usuarios a terceros. Para la gestión del Servicio, encarga las siguientes tareas.',
      ],
      bullets: [
        'AI3 Inc. (BizRouter): intermediación de las solicitudes de IA — orientación automática de la oficina internacional, traducción de mensajes de consulta, avisos y anuncios de segunda mano, y respuestas de la ayuda del Servicio',
        'Anthropic PBC: generación de respuestas y traducciones de IA (tratadas a través de BizRouter)',
        'Google LLC: autenticación del inicio de sesión con Google y envío de correos mediante Gmail (códigos de verificación del correo electrónico de la universidad y avisos al personal responsable de la oficina internacional)',
        'Apple Inc.: autenticación del inicio de sesión con Apple y desvinculación de Apple al darte de baja',
        'Cloudflare, Inc.: intermediación del acceso al Servicio (seguridad de la transmisión) y reenvío de los correos de consulta',
      ],
    },
    {
      heading: '7. Transferencia de datos personales al extranjero',
      paragraphs: [
        'Para realizar las tareas encargadas, los datos personales se transfieren al extranjero como se indica a continuación. Todas las transmisiones se realizan cada vez que usas la función correspondiente, a través de una red cifrada (HTTPS). El plazo de conservación del destinatario se rige por la política de privacidad y las condiciones del servicio de cada empresa.',
      ],
      bullets: [
        'Anthropic PBC (Estados Unidos, privacy@anthropic.com): preguntas a la oficina internacional y conversación anterior (hasta 20), avisos y ayuda utilizados como base de las respuestas, y texto de los mensajes, avisos y anuncios de segunda mano que se van a traducir — generación de respuestas y traducciones de IA',
        'Google LLC (Estados Unidos): datos de la cuenta al iniciar sesión con Google, y la dirección de destino y el contenido de los correos que se envían (código de verificación, primera pregunta de la consulta) — autenticación del inicio de sesión y envío de correos',
        'Apple Inc. (Estados Unidos): datos de verificación del inicio de sesión con Apple y el token utilizado para la desvinculación al darte de baja — autenticación del inicio de sesión y desvinculación',
        'Cloudflare, Inc. (Estados Unidos): las solicitudes que se intercambian al acceder al Servicio (incluida la dirección IP) y los correos de consulta — intermediación del acceso y reenvío de correos',
        'Cómo rechazarlo: si no usas las funciones de IA ni el inicio de sesión con Google o Apple, esos datos no se transfieren. La intermediación del acceso a través de Cloudflare es imprescindible para usar el Servicio; si la rechazas, no podrás usarlo.',
      ],
    },
    {
      heading: '8. Derechos de los usuarios',
      paragraphs: [
        'Puedes, en cualquier momento, acceder a tus datos personales, rectificarlos o eliminarlos, o solicitar la suspensión de su tratamiento, ya sea modificándolos directamente en la pantalla de tu perfil o solicitándolo a través del contacto indicado más abajo. Puedes darte de baja desde la pantalla de tu perfil y, en las consultas de la oficina internacional, puedes solicitar su finalización en cualquier momento desde la sala de consulta.',
      ],
    },
    {
      heading: '9. Cookies y almacenamiento local',
      paragraphs: [
        'Para mantener la sesión iniciada, almacenamos un token de actualización en una cookie httpOnly, y guardamos la configuración del idioma de la interfaz en una cookie y en el almacenamiento del navegador. No utilizamos cookies con fines publicitarios ni de seguimiento. Puedes rechazar las cookies en la configuración de tu navegador, pero es posible que entonces no se mantenga la sesión iniciada.',
      ],
    },
    {
      heading: '10. Enlaces a vídeos externos y Servicios de API de YouTube',
      paragraphs: [
        'El Servicio utiliza los Servicios de API de YouTube para elaborar la lista de enlaces a vídeos de YouTube que se muestra en las páginas de informes universitarios. En este proceso, el Servicio no envía los datos personales de los usuarios a YouTube ni a Google, ni recopila información de los usuarios a través de YouTube.',
        'No obstante, como el navegador del usuario carga las imágenes de vista previa de los vídeos directamente desde los servidores de imágenes de YouTube, al abrir una página que contenga una lista de vídeos pueden transmitirse a Google la dirección IP de acceso y la información del navegador. El tratamiento de la información una vez que pulsas un enlace y accedes a YouTube se rige por la Política de Privacidad de Google.',
      ],
      links: [{ label: 'Política de Privacidad de Google', href: GOOGLE_PRIVACY_URL }],
    },
    {
      heading: '11. Medidas de seguridad',
      paragraphs: [
        'Las contraseñas se almacenan únicamente como hashes que no pueden descifrarse, los tokens de inicio de sesión se almacenan como hashes y las transmisiones se cifran mediante HTTPS. El acceso de administración se controla mediante una autenticación independiente y el registro de los accesos.',
        'Las fotos que subes a publicaciones y chats se almacenan tal cual, por lo que la información contenida en la foto, como el lugar donde se tomó, puede conservarse. Si no quieres que se revele la ubicación, elimina los datos de ubicación de la foto antes de subirla. En las fotos de artículos de segunda mano, el Servicio elimina esta información al volver a guardarlas.',
      ],
    },
    {
      heading: '12. Responsable de la protección de datos personales y contacto',
      paragraphs: [
        `Responsable de la protección de datos personales: el operador del Servicio · Contacto: ${LEGAL_CONTACT}`,
        'Para denuncias y consultas sobre vulneraciones de datos personales, puedes dirigirte al Centro de Denuncias de Vulneraciones de Información Personal (privacy.kisa.or.kr, 118) o al Comité de Mediación de Controversias sobre Información Personal (kopico.go.kr, 1833-6972).',
      ],
    },
    {
      heading: '13. Modificación de la política',
      paragraphs: [
        'Cuando modifiquemos esta política, lo anunciaremos en el Servicio a partir de 7 días antes de su entrada en vigor. Si los datos recopilados, las finalidades o los plazos de conservación cambian de forma desfavorable para los usuarios, lo anunciaremos con 30 días de antelación.',
      ],
    },
    {
      heading: '14. Idioma',
      paragraphs: ['Esta política se redacta en coreano. Las traducciones a otros idiomas se ofrecen por comodidad; si una traducción y el original en coreano difieren, prevalece el original en coreano.'],
    },
  ],
}
