import type { LegalDocument } from '../legalContent'
import { GOOGLE_PRIVACY_URL, LEGAL_CONTACT_PLACEHOLDER } from '../legalConstants'

/** 개인정보처리방침 프랑스어 번역. 정본은 legalContent.ts 의 한국어(ko). 원문을 바꾸면 이 파일도 같이 고치고 version 을 맞춘다. */
export const PRIVACY_FR: LegalDocument = {
  version: 'v3',
  updatedAt: '2026-11-25',
  title: 'Politique de confidentialité',
  intro:
    'Conformément à la loi sur la protection des informations personnelles (Personal Information Protection Act) de la République de Corée, Ovlo (ci-après le « Service ») adopte la présente politique de confidentialité afin de protéger les données personnelles des utilisateurs et de traiter rapidement les réclamations qui s’y rapportent.',
  sections: [
    {
      heading: '1. Données personnelles collectées',
      paragraphs: ['Le Service collecte les données suivantes lors de l’inscription et de l’utilisation du Service.'],
      bullets: [
        'Obligatoires : adresse e-mail, mot de passe (conservé uniquement sous forme de hachage), pseudo, nom, lieu d’origine, université d’appartenance, spécialité, cursus et année d’études',
        'Facultatives : photo de profil, présentation, date de naissance, coordonnées, compétences linguistiques, expériences d’études à l’étranger',
        'En cas de connexion via un réseau social (Google) : adresse e-mail du compte Google, nom, identifiant d’utilisateur Google',
        'En cas de vérification de l’adresse e-mail universitaire : adresse e-mail universitaire vérifiée, date et heure de la vérification, université vérifiée',
        'Données générées lors de l’utilisation du Service : publications, commentaires, messages de chat et fichiers joints, journaux de connexion (adresse IP, date et heure de connexion, chemin de la requête), informations sur l’appareil et le navigateur',
        'Cookies : jeton d’actualisation (httpOnly) permettant de maintenir la connexion, paramètre de langue d’affichage',
      ],
    },
    {
      heading: '2. Finalités de la collecte et de l’utilisation des données personnelles',
      paragraphs: ['Les données personnelles collectées sont utilisées uniquement aux fins suivantes.'],
      bullets: [
        'Identification des membres, connexion, vérification de l’identité (vérification de l’adresse e-mail universitaire)',
        'Fourniture et exploitation des fonctionnalités communautaires telles que les tableaux et le chat',
        'Fourniture d’informations sur les universités et les échanges, adaptation de la langue d’affichage',
        'Prévention des utilisations frauduleuses, réponse aux incidents de sécurité, conservation des journaux de connexion',
        'Réponse aux demandes et diffusion des avis',
      ],
    },
    {
      heading: '3. Fonctionnalité d’orientation et de consultation du service des relations internationales de l’université (en préparation)',
      paragraphs: [
        'Le Service prépare une fonctionnalité qui oriente les étudiants internationaux à partir de documents approuvés par le service des relations internationales de l’université et qui, uniquement avec le consentement de l’utilisateur, transmet le contenu de sa demande au personnel du service des relations internationales de l’université concernée.',
        'Lorsque cette fonctionnalité sera ouverte : ① les questions saisies par l’utilisateur et les réponses automatiques seront enregistrées afin de mesurer la qualité du Service ; ② le texte des questions pourra être confié, pour traitement, à un prestataire externe de services d’IA afin d’y répondre (aucune information d’identification telle que le nom ou l’adresse e-mail n’est transmise) ; ③ le contenu transmis au personnel et l’étendue de cette transmission seront indiqués sur un écran de consentement distinct au moment de la transmission. Avant l’ouverture de la fonctionnalité, la présente politique sera mise à jour pour préciser les sous-traitants concernés et les durées de conservation.',
      ],
    },
    {
      heading: '4. Durée de conservation et d’utilisation',
      paragraphs: [
        'Les données personnelles sont détruites sans délai une fois atteintes les finalités de leur collecte et de leur utilisation. Toutefois, les données suivantes sont conservées pendant la durée indiquée.',
      ],
      bullets: [
        'Informations du membre : jusqu’à la suppression du compte. Dès la suppression du compte, les informations du compte sont supprimées ou rendues non identifiables, et les publications et commentaires rédigés sont rendus non publics.',
        'Journaux de connexion (IP, date et heure de connexion) : 3 mois (loi sur la protection du secret des communications)',
        'Enregistrements de vérification de l’adresse e-mail universitaire : expirent 1 an après la date de vérification et sont supprimés lors de la suppression du compte.',
        'Enregistrements d’utilisation frauduleuse : 1 an, afin de prévenir toute récidive',
      ],
    },
    {
      heading: '5. Communication à des tiers et sous-traitance du traitement',
      paragraphs: [
        'Le Service ne vend ni ne communique les données personnelles des utilisateurs à des tiers. Pour l’exploitation du Service, les tâches suivantes sont confiées à des sous-traitants.',
      ],
      bullets: [
        'Google LLC : authentification par connexion via un réseau social (Google OAuth)',
        'Prestataire d’envoi d’e-mails : envoi des codes de vérification de l’adresse e-mail universitaire',
        'Prestataire externe de services d’IA : traitement du texte des questions lorsque la fonctionnalité d’orientation du service des relations internationales (section 3) sera ouverte. Le sous-traitant sera indiqué dans la présente politique avant le lancement de la fonctionnalité.',
      ],
    },
    {
      heading: '6. Droits des utilisateurs',
      paragraphs: [
        'Vous pouvez à tout moment demander l’accès à vos données personnelles, leur rectification ou leur suppression, ou la suspension de leur traitement, soit en les modifiant directement depuis l’écran de votre profil, soit en adressant une demande aux coordonnées ci-dessous. Vous pouvez supprimer votre compte depuis l’écran de votre profil.',
      ],
    },
    {
      heading: '7. Cookies et stockage local',
      paragraphs: [
        'Afin de maintenir votre connexion, un jeton d’actualisation est enregistré dans un cookie httpOnly, et le paramètre de langue d’affichage est conservé dans un cookie et dans le stockage du navigateur. Aucun cookie publicitaire ou de suivi n’est utilisé. Vous pouvez refuser les cookies dans les paramètres de votre navigateur, mais votre connexion pourrait alors ne pas être maintenue.',
      ],
    },
    {
      heading: '8. Liens vers des vidéos externes et services d’API YouTube',
      paragraphs: [
        'Le Service utilise les services d’API YouTube pour établir la liste des liens vers des vidéos YouTube affichée sur les pages de rapport des universités. Ce faisant, le Service ne transmet pas vos données personnelles à YouTube ni à Google et ne collecte pas non plus d’informations vous concernant par l’intermédiaire de YouTube.',
        'Toutefois, les images d’aperçu des vidéos étant chargées par votre navigateur directement depuis les serveurs d’images de YouTube, l’ouverture d’une page comportant une liste de vidéos peut entraîner la transmission à Google de votre adresse IP et des informations relatives à votre navigateur. Le traitement des informations après que vous avez cliqué sur un lien et êtes redirigé vers YouTube est régi par les Règles de confidentialité de Google.',
      ],
      links: [{ label: 'Règles de confidentialité de Google', href: GOOGLE_PRIVACY_URL }],
    },
    {
      heading: '9. Mesures de sécurité',
      paragraphs: [
        'Les mots de passe sont conservés uniquement sous forme de hachage irréversible, les jetons de connexion sont conservés sous forme de hachage, et les transmissions sont chiffrées au moyen du protocole HTTPS. L’accès administrateur est contrôlé par une authentification distincte et par l’enregistrement des accès.',
      ],
    },
    {
      heading: '10. Responsable de la protection des données personnelles et contact',
      paragraphs: [
        `Responsable de la protection des données personnelles : l’exploitant du Service · Contact : ${LEGAL_CONTACT_PLACEHOLDER}`,
        'Pour tout signalement ou toute consultation concernant une atteinte aux données personnelles, vous pouvez vous adresser au Centre de signalement des atteintes aux données personnelles (Personal Information Infringement Report Center ; privacy.kisa.or.kr, 118) ou au Comité de médiation des litiges relatifs aux données personnelles (Personal Information Dispute Mediation Committee ; kopico.go.kr, 1833-6972).',
      ],
    },
    {
      heading: '11. Modification de la politique',
      paragraphs: [
        'Toute modification de la présente politique est annoncée par un avis dans le Service à partir de 7 jours avant son entrée en vigueur. Lorsque les données collectées, les finalités ou les durées de conservation sont modifiées de manière défavorable aux utilisateurs, la modification est annoncée 30 jours à l’avance.',
      ],
    },
  ],
}
