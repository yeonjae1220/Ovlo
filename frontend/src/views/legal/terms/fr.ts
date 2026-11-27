import type { LegalDocument } from '../legalContent'
import { LEGAL_CONTACT, YOUTUBE_TERMS_URL } from '../legalConstants'

/** 이용약관 프랑스어 번역. 정본은 legalContent.ts 의 한국어(ko). 원문을 바꾸면 이 파일도 같이 고치고 version 을 맞춘다. */
export const TERMS_FR: LegalDocument = {
  version: 'v3',
  updatedAt: '2026-11-25',
  title: 'Conditions d’utilisation',
  intro:
    'Les présentes conditions définissent les conditions et modalités d’utilisation d’Ovlo (ci-après le « Service »), ainsi que les droits et obligations des utilisateurs et du Service.',
  sections: [
    {
      heading: '1. Contenu du Service',
      paragraphs: [
        'Le Service fournit aux étudiants en échange et aux étudiants internationaux des informations sur les universités, des fonctionnalités communautaires (tableaux et chat), la vérification de l’adresse e-mail universitaire, ainsi qu’une fonctionnalité d’orientation et de mise en relation pour des consultations fondée sur des documents approuvés par le service des relations internationales de l’université (en préparation).',
      ],
    },
    {
      heading: '2. Compte',
      paragraphs: [
        'Vous devez vous inscrire avec des informations exactes et êtes responsable de la gestion de votre compte et de votre mot de passe. Vous ne pouvez pas vous inscrire avec les informations d’une autre personne, ni céder ou prêter votre compte. La vérification de l’adresse e-mail universitaire confirme uniquement que vous avez accès à cette adresse ; elle ne garantit ni votre inscription effective dans l’établissement ni l’exactitude de ce que vous publiez.',
      ],
    },
    {
      heading: '3. Nature des informations et limitation de responsabilité',
      paragraphs: [
        'Les informations fournies par le Service, notamment les rapports sur les universités, sont des documents de référence établis par IA à partir de sources publiques et de documents officiels des universités ; leur source et leur date de référence sont indiquées. Pour les points importants tels que les visas, la scolarité ou les montants, vous devez impérativement vous référer aux informations officielles de l’organisme concerné ; le Service ne garantit ni l’exactitude ni l’actualité des informations.',
        'La fonctionnalité d’orientation du service des relations internationales ne répond que dans les limites des documents approuvés par l’université concernée et ne porte aucune appréciation sur votre situation personnelle (par exemple l’éligibilité à un titre de séjour). Ces indications ne constituent ni une décision officielle de l’université ni un conseil juridique.',
      ],
    },
    {
      heading: '4. Contenus publiés par les utilisateurs',
      paragraphs: [
        'Les droits sur les publications, commentaires, messages et fichiers que vous publiez vous appartiennent. Vous autorisez le Service à les utiliser dans la mesure nécessaire pour les afficher, les stocker, les transmettre et les traduire au sein du Service. Vous ne devez pas publier les œuvres ou les données personnelles d’autrui sans leur consentement.',
      ],
    },
    {
      heading: '5. Comportements interdits',
      paragraphs: [
        'Les comportements suivants sont interdits ; toute infraction peut entraîner la suppression du contenu, la restriction de l’utilisation ou la suppression du compte.',
      ],
      bullets: [
        'Usurper l’identité d’autrui, tenter une vérification avec de fausses informations, partager un compte',
        'Publier des injures, des propos haineux ou discriminatoires, du harcèlement, des contenus obscènes ou des informations illicites',
        'Publier de la publicité, du spam ou des contenus répétés à des fins lucratives (à l’exception des offres partenaires signalées comme telles par le Service)',
        'Contourner la sécurité du Service ou collecter des données en masse par des moyens automatisés',
        'Utiliser la fonctionnalité d’orientation et de consultation du service des relations internationales pour de faux signalements, des canulars ou des menaces',
      ],
    },
    {
      heading: '6. Tolérance zéro envers les contenus choquants et les utilisateurs abusifs',
      paragraphs: [
        'Le Service ne tolère aucun contenu choquant ou inapproprié, ni aucun utilisateur qui harcèle d’autres utilisateurs. Vous devez accepter ce principe lors de votre inscription ; il s’applique à tout contenu publié par les membres, notamment les publications, commentaires, messages de chat et profils. Les contenus suivants sont en particulier interdits :',
      ],
      bullets: [
        'Spam, publicité, arnaques ou publication répétée d’un même contenu',
        'Harcèlement, intimidation, menaces ou insultes visant une personne en particulier',
        'Discours haineux et discrimination fondés sur l’origine raciale ou ethnique, la nationalité, la religion, le sexe, l’orientation sexuelle, le handicap ou toute autre caractéristique similaire',
        'Contenus pornographiques ou sexuellement explicites, et tout contenu à caractère sexuel impliquant des mineurs',
        'Contenus incitant à la violence ou à l’automutilation, ou en faisant l’apologie',
        'Commerce de biens ou services illicites, et tout autre contenu contraire à la loi',
        'Divulgation sans consentement des coordonnées, de l’adresse, des pièces d’identité ou d’autres données personnelles d’autrui',
      ],
    },
    {
      heading: '7. Signalement, blocage et délais de traitement',
      paragraphs: [
        'Vous pouvez signaler les contenus ou utilisateurs mentionnés ci-dessus à l’aide de la fonction « Signaler » disponible sur les publications, commentaires, messages de chat et profils des membres, et bloquer un membre à l’aide de la fonction « Bloquer ». Lorsque vous bloquez un membre, vous ne voyez plus les publications et commentaires l’un de l’autre, vous ne pouvez plus échanger de messages et les abonnements entre vous sont supprimés. Vous pouvez lever un blocage à tout moment depuis l’écran « Membres bloqués » de votre profil.',
        'Les signalements reçus sont examinés par un modérateur dans un délai de 24 heures ; lorsqu’une infraction est constatée, le contenu concerné est supprimé et l’utilisation de l’utilisateur fautif est restreinte, ou son compte est suspendu ou supprimé. Les faux signalements et l’usage abusif de la fonction de signalement constituent également des motifs de restriction de l’utilisation.',
      ],
    },
    {
      heading: '8. Offres partenaires',
      paragraphs: [
        'Le Service peut présenter, sous la mention « Partenaire », des services externes utiles à votre installation, tels que cartes SIM, assurances, logement ou accueil à l’aéroport, et percevoir une commission lorsque vous les utilisez. Cette mention est toujours affichée et n’apparaît jamais dans le corps des indications approuvées par le service des relations internationales. Les contrats relatifs aux services externes et la responsabilité qui en découle incombent aux prestataires concernés.',
      ],
    },
    {
      heading: '9. Liens vers des vidéos externes (YouTube)',
      paragraphs: [
        'Sur les pages de rapport d’une université, le Service affiche des liens vers des vidéos YouTube publiques liées à cette université, accompagnés de leur titre, du nom de la chaîne et d’une image d’aperçu. Les vidéos ne sont ni stockées ni lues au sein du Service ; cliquer sur un lien vous redirige vers YouTube. Cette fonctionnalité utilise les services d’API YouTube, et en l’utilisant, vous êtes réputé accepter également les Conditions d’utilisation de YouTube.',
        `Le contenu de chaque vidéo et les droits qui s’y rattachent appartiennent à son auteur, et le Service ne garantit pas le contenu des vidéos. Les liens vers des vidéos supprimées ou devenues privées sont vérifiés périodiquement et retirés. Si l’auteur d’une vidéo ou une personne qui y apparaît demande le retrait d’un lien, celui-ci est retiré sans délai. Demandes : ${LEGAL_CONTACT}`,
      ],
      links: [{ label: 'Conditions d’utilisation de YouTube', href: YOUTUBE_TERMS_URL }],
    },
    {
      heading: '10. Modification et interruption du Service',
      paragraphs: [
        'Le Service peut ajouter, modifier ou supprimer des fonctionnalités ; les modifications importantes font l’objet d’un avis préalable. Le Service peut être temporairement interrompu pour des raisons inévitables telles qu’une catastrophe naturelle ou une panne du système.',
      ],
    },
    {
      heading: '11. Résiliation',
      paragraphs: [
        'Vous pouvez supprimer votre compte à tout moment depuis l’écran de votre profil. Vos données personnelles sont alors traitées conformément à la politique de confidentialité.',
      ],
    },
    {
      heading: '12. Droit applicable et litiges',
      paragraphs: [
        'Les présentes conditions sont régies par le droit de la République de Corée, et les litiges relèvent du tribunal compétent en vertu de la loi sur la procédure civile.',
      ],
    },
    {
      heading: '13. Modification des conditions',
      paragraphs: [
        `Toute modification des présentes conditions est annoncée 7 jours avant son entrée en vigueur (30 jours pour les modifications défavorables aux utilisateurs). Contact : ${LEGAL_CONTACT}`,
      ],
    },
    {
      heading: '14. Langue',
      paragraphs: ['Les présentes conditions sont rédigées en coréen. Des traductions dans d’autres langues sont fournies à titre indicatif ; en cas de divergence entre une traduction et l’original coréen, l’original coréen prévaut.'],
    },
  ],
}
