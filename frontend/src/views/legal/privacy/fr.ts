import type { LegalDocument } from '../legalContent'
import { GOOGLE_PRIVACY_URL, LEGAL_CONTACT } from '../legalConstants'

/** 개인정보처리방침 프랑스어 번역. 정본은 legalContent.ts 의 한국어(ko). 원문을 바꾸면 이 파일도 같이 고치고 version 을 맞춘다. */
export const PRIVACY_FR: LegalDocument = {
  version: 'v4',
  updatedAt: '2026-11-28',
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
        'En cas de connexion via Google : adresse e-mail du compte Google, nom, identifiant d’utilisateur Google',
        'En cas de connexion via Apple : identifiant d’utilisateur Apple, adresse e-mail communiquée par Apple (l’adresse relais d’Apple si vous choisissez de masquer votre e-mail), nom que l’utilisateur a indiqué',
        'En cas de vérification de l’adresse e-mail universitaire : adresse e-mail universitaire vérifiée, date et heure de la vérification, université vérifiée',
        'En cas d’utilisation de l’orientation et des consultations du service des relations internationales de l’université : questions et réponses automatiques, annonces citées dans les réponses, évaluations des réponses, texte du consentement donné lors d’une demande de consultation et sa version, messages échangés dans le salon de consultation et leurs traductions',
        'En cas d’utilisation des annonces de seconde main : annonces de vente (titre, description, prix, zone, université, lieu de remise, dates possibles), photos des articles, chats de transaction, motifs de signalement',
        'En cas d’utilisation du calendrier : événements personnels (titre, description, lieu, horaires), contenu des demandes de gestion du calendrier d’une université (service, e-mail de contact, message)',
        'En cas de signalement ou de blocage : cible du signalement, motif et description, membres bloqués',
        'En cas d’envoi d’un avis : contenu de l’avis, e-mail de réponse s’il est indiqué, version de l’application et informations sur le navigateur',
        'Données générées lors de l’utilisation du Service : publications, commentaires, messages de chat, fichiers joints, journaux de connexion (adresse IP, date et heure de connexion, chemin de la requête, numéro de membre), informations sur l’appareil et le navigateur',
        'Cookies : jeton d’actualisation (httpOnly) permettant de maintenir la connexion, paramètre de langue d’affichage',
      ],
    },
    {
      heading: '2. Finalités de la collecte et de l’utilisation des données personnelles',
      paragraphs: ['Les données personnelles collectées sont utilisées uniquement aux fins suivantes.'],
      bullets: [
        'Identification des membres, connexion, vérification de l’identité (vérification de l’adresse e-mail universitaire)',
        'Fourniture et exploitation des fonctionnalités communautaires telles que les tableaux, le chat, les annonces de seconde main et le calendrier',
        'Orientation automatique fondée sur les annonces du service des relations internationales de l’université, mise en relation avec le personnel de ce service pour des consultations, aide à l’utilisation du Service',
        'Traduction des annonces, des messages de chat et des annonces de seconde main, fourniture d’informations sur les universités et les échanges, adaptation de la langue d’affichage',
        'Prévention des utilisations frauduleuses, traitement des signalements, réponse aux incidents de sécurité, conservation des journaux de connexion',
        'Réponse aux demandes et diffusion des avis',
      ],
    },
    {
      heading: '3. Fonctionnalités utilisant l’IA',
      paragraphs: [
        'Les fonctionnalités suivantes du Service font appel à un service d’IA externe pour produire des réponses ou des traductions. Au service d’IA externe, nous n’envoyons que le texte de la question ou du message ainsi que les annonces et les articles d’aide nécessaires pour répondre ; nous n’envoyons aucune information d’identification telle que le nom, l’adresse e-mail ou le numéro de membre. Toutefois, ce que l’utilisateur écrit lui-même dans le texte est transmis tel quel ; nous vous recommandons donc de ne pas y indiquer de données personnelles sensibles.',
      ],
      bullets: [
        'Orientation automatique du service des relations internationales : les étudiants ayant vérifié leur adresse e-mail universitaire reçoivent des réponses fondées sur les annonces approuvées par le service des relations internationales de leur université. Les questions et les réponses sont enregistrées afin de contrôler la qualité des réponses et de pouvoir les poursuivre dans une consultation (durée de conservation : section 5).',
        'Mise en relation avec le service des relations internationales : uniquement si l’utilisateur y consent sur un écran distinct, le contenu de la conversation est transmis au personnel du service des relations internationales de son université. Le personnel voit les questions et les réponses automatiques de la conversation choisie (20 échanges au maximum), la langue utilisée, l’état de vérification de l’adresse e-mail universitaire, le pseudo et la photo de profil affichés dans le salon de consultation, ainsi que les messages ultérieurs du salon ; l’e-mail de notification d’une nouvelle consultation contient aussi la première question. L’adresse e-mail et le nom ne sont pas transmis. Les messages du salon sont traduits dans la langue de chacun.',
        'Aide du Service : lorsqu’un membre connecté demande comment utiliser le Service, la réponse s’appuie sur les articles d’aide rédigés par le Service. Le texte de la question n’est traité que le temps de produire la réponse et n’est pas enregistré.',
        'Traduction : les annonces du service des relations internationales et les annonces de seconde main sont traduites dans la langue de l’utilisateur.',
      ],
    },
    {
      heading: '4. Informations visibles par le personnel du service des relations internationales',
      paragraphs: [
        'Le personnel du service des relations internationales est composé de membres du Service invités par le service des relations internationales de l’université concernée. Il ne peut voir que les consultations adressées à sa propre université et, une fois retiré de cette fonction, il n’a plus accès aux salons de consultation. La liste des consultations que le personnel peut télécharger contient l’état, les horaires, la langue et la première question de chaque consultation, mais ni l’adresse e-mail ni le nom de l’étudiant.',
      ],
    },
    {
      heading: '5. Durée de conservation et d’utilisation',
      paragraphs: [
        'Les données personnelles sont détruites sans délai une fois atteintes les finalités de leur collecte et de leur utilisation. Toutefois, les données suivantes sont conservées pendant la durée indiquée.',
      ],
      bullets: [
        'Informations du membre : jusqu’à la suppression du compte. Dès la suppression du compte, les informations du compte sont immédiatement rendues non identifiables et les fichiers téléversés (photo de profil, fichiers joints) sont supprimés. Les publications, commentaires, messages de chat et annonces de seconde main ne sont pas supprimés mais rendus invisibles aux autres utilisateurs. Un compte supprimé ne peut pas être rétabli.',
        'Enregistrements de vérification de l’adresse e-mail universitaire : expirent 1 an après la date de vérification et sont supprimés lors de la suppression du compte.',
        'Questions, réponses et consultations du service des relations internationales : le texte est supprimé 1 an après le jour de la question (pour une consultation, après le jour de son ouverture) et immédiatement lors de la suppression du compte. L’heure d’utilisation et le résultat du traitement subsistent à des fins statistiques.',
        'Questions adressées à l’aide du Service : non enregistrées. Seul un compteur servant à limiter le nombre d’utilisations est conservé, au maximum une journée.',
        'Événements personnels du calendrier et demandes de gestion du calendrier d’une université : supprimés lors de la suppression du compte.',
        'Enregistrements de signalement : 1 an après le jour où le traitement est terminé (pour un signalement d’annonce de seconde main, après le jour de sa réception), le membre auteur du signalement, la cible et le motif sont effacés ; seuls le type de signalement, le résultat du traitement et la date subsistent à des fins statistiques.',
        'Enregistrements de blocage : jusqu’à la levée du blocage ou jusqu’à la suppression du compte de l’un des deux membres',
        'Journaux de connexion (adresse IP, date et heure de connexion, chemin de la requête, numéro de membre) : 3 mois (loi sur la protection du secret des communications)',
        'Adresses IP et adresses e-mail servant à limiter le nombre de tentatives de connexion, d’inscription et de recherche : supprimées automatiquement en moins de 10 minutes',
        'Sauvegardes de la base de données : des sauvegardes sont conservées séparément pour permettre la reprise après incident ; les informations supprimées peuvent donc subsister dans les sauvegardes jusqu’à la suppression successive de celles-ci. Les sauvegardes ne sont utilisées que pour la restauration.',
      ],
    },
    {
      heading: '6. Sous-traitance du traitement',
      paragraphs: [
        'Le Service ne vend ni ne communique les données personnelles des utilisateurs à des tiers. Pour l’exploitation du Service, les tâches suivantes sont confiées à des sous-traitants.',
      ],
      bullets: [
        'AI3 Inc. (BizRouter) : relais des requêtes d’IA — orientation automatique du service des relations internationales, traduction des messages de consultation, des annonces et des annonces de seconde main, réponses de l’aide du Service',
        'Anthropic PBC : production des réponses et des traductions par l’IA (traitées via BizRouter)',
        'Google LLC : authentification par connexion Google, envoi d’e-mails via Gmail (codes de vérification de l’adresse e-mail universitaire, notifications au personnel du service des relations internationales)',
        'Apple Inc. : authentification par connexion Apple et déconnexion d’Apple lors de la suppression du compte',
        'Cloudflare, Inc. : relais des connexions au Service (sécurité des transmissions) et transmission des e-mails de demande',
      ],
    },
    {
      heading: '7. Transfert de données personnelles à l’étranger',
      paragraphs: [
        'Pour exécuter les tâches confiées, des données personnelles sont transférées à l’étranger comme indiqué ci-dessous. Chaque transfert a lieu chaque fois que l’utilisateur emploie la fonctionnalité concernée, sur un réseau chiffré (HTTPS). La durée de conservation chez les destinataires est régie par la politique de confidentialité et les conditions de service de chaque société.',
      ],
      bullets: [
        'Anthropic PBC (États-Unis, privacy@anthropic.com) : questions adressées au service des relations internationales et échanges précédents (20 au maximum), annonces et articles d’aide servant de fondement aux réponses, texte des messages, annonces et annonces de seconde main à traduire — production des réponses et des traductions par l’IA',
        'Google LLC (États-Unis) : informations du compte lors de la connexion Google, adresse du destinataire et contenu des e-mails envoyés (code de vérification, première question d’une consultation) — authentification de connexion et envoi d’e-mails',
        'Apple Inc. (États-Unis) : informations de vérification de la connexion Apple et jeton servant à la déconnexion lors de la suppression du compte — authentification de connexion et déconnexion',
        'Cloudflare, Inc. (États-Unis) : requêtes échangées lors de la connexion au Service (y compris l’adresse IP) et e-mails de demande — relais des connexions et transmission des e-mails',
        'Modalités de refus : si vous n’utilisez pas les fonctionnalités d’IA ni la connexion Google ou Apple, les informations correspondantes ne sont pas transférées. Le relais des connexions via Cloudflare est indispensable à l’utilisation du Service ; si vous le refusez, vous ne pouvez pas utiliser le Service.',
      ],
    },
    {
      heading: '8. Droits des utilisateurs',
      paragraphs: [
        'Vous pouvez à tout moment demander l’accès à vos données personnelles, leur rectification ou leur suppression, ou la suspension de leur traitement, soit en les modifiant directement depuis l’écran de votre profil, soit en adressant une demande aux coordonnées ci-dessous. Vous pouvez supprimer votre compte depuis l’écran de votre profil et demander à tout moment la clôture d’une consultation du service des relations internationales depuis le salon de consultation.',
      ],
    },
    {
      heading: '9. Cookies et stockage local',
      paragraphs: [
        'Afin de maintenir votre connexion, un jeton d’actualisation est enregistré dans un cookie httpOnly, et le paramètre de langue d’affichage est conservé dans un cookie et dans le stockage du navigateur. Aucun cookie publicitaire ou de suivi n’est utilisé. Vous pouvez refuser les cookies dans les paramètres de votre navigateur, mais votre connexion pourrait alors ne pas être maintenue.',
      ],
    },
    {
      heading: '10. Liens vers des vidéos externes et services d’API YouTube',
      paragraphs: [
        'Le Service utilise les services d’API YouTube pour établir la liste des liens vers des vidéos YouTube affichée sur les pages de rapport des universités. Ce faisant, le Service ne transmet pas vos données personnelles à YouTube ni à Google et ne collecte pas non plus d’informations vous concernant par l’intermédiaire de YouTube.',
        'Toutefois, les images d’aperçu des vidéos étant chargées par votre navigateur directement depuis les serveurs d’images de YouTube, l’ouverture d’une page comportant une liste de vidéos peut entraîner la transmission à Google de votre adresse IP et des informations relatives à votre navigateur. Le traitement des informations après que vous avez cliqué sur un lien et êtes redirigé vers YouTube est régi par les Règles de confidentialité de Google.',
      ],
      links: [{ label: 'Règles de confidentialité de Google', href: GOOGLE_PRIVACY_URL }],
    },
    {
      heading: '11. Mesures de sécurité',
      paragraphs: [
        'Les mots de passe sont conservés uniquement sous forme de hachage irréversible, les jetons de connexion sont conservés sous forme de hachage, et les transmissions sont chiffrées au moyen du protocole HTTPS. L’accès administrateur est contrôlé par une authentification distincte et par l’enregistrement des accès.',
        'Les photos publiées dans les publications et les chats sont conservées telles quelles ; les informations qu’elles contiennent, comme le lieu de prise de vue, peuvent donc subsister. Si vous ne souhaitez pas que votre position soit révélée, supprimez les données de localisation de la photo avant de la publier. Les photos des articles de seconde main sont réenregistrées par le Service, qui supprime ces informations.',
      ],
    },
    {
      heading: '12. Responsable de la protection des données personnelles et contact',
      paragraphs: [
        `Responsable de la protection des données personnelles : l’exploitant du Service · Contact : ${LEGAL_CONTACT}`,
        'Pour tout signalement ou toute consultation concernant une atteinte aux données personnelles, vous pouvez vous adresser au Centre de signalement des atteintes aux données personnelles (Personal Information Infringement Report Center ; privacy.kisa.or.kr, 118) ou au Comité de médiation des litiges relatifs aux données personnelles (Personal Information Dispute Mediation Committee ; kopico.go.kr, 1833-6972).',
      ],
    },
    {
      heading: '13. Modification de la politique',
      paragraphs: [
        'Toute modification de la présente politique est annoncée par un avis dans le Service à partir de 7 jours avant son entrée en vigueur. Lorsque les données collectées, les finalités ou les durées de conservation sont modifiées de manière défavorable aux utilisateurs, la modification est annoncée 30 jours à l’avance.',
      ],
    },
    {
      heading: '14. Langue',
      paragraphs: ['La présente politique est rédigée en coréen. Des traductions dans d’autres langues sont fournies à titre indicatif ; en cas de divergence entre une traduction et l’original coréen, l’original coréen prévaut.'],
    },
  ],
}
