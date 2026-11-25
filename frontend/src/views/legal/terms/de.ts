import type { LegalDocument } from '../legalContent'
import { LEGAL_CONTACT_PLACEHOLDER, YOUTUBE_TERMS_URL } from '../legalConstants'

/** 이용약관 독일어 번역. 정본은 legalContent.ts 의 한국어(ko). 원문을 바꾸면 이 파일도 같이 고치고 version 을 맞춘다. */
export const TERMS_DE: LegalDocument = {
  version: 'v3',
  updatedAt: '2026-11-25',
  title: 'Nutzungsbedingungen',
  intro:
    'Diese Nutzungsbedingungen regeln die Bedingungen und Verfahren für die Nutzung von Ovlo (nachfolgend „Dienst“) sowie die Rechte und Pflichten der Nutzer und des Dienstes.',
  sections: [
    {
      heading: '1. Inhalt des Dienstes',
      paragraphs: [
        'Der Dienst bietet Austauschstudierenden und internationalen Studierenden Informationen zu Hochschulen, Community-Funktionen (Boards und Chat), die Verifizierung der Hochschul-E-Mail-Adresse sowie eine Funktion für Auskünfte und die Vermittlung von Beratungen auf Grundlage von Materialien, die von den International Offices der Hochschulen freigegeben wurden (in Vorbereitung).',
      ],
    },
    {
      heading: '2. Konto',
      paragraphs: [
        'Du musst dich mit zutreffenden Angaben registrieren und bist selbst für die Verwaltung deines Kontos und Passworts verantwortlich. Du darfst dich nicht mit den Daten einer anderen Person registrieren und dein Konto weder übertragen noch verleihen. Die Verifizierung der Hochschul-E-Mail-Adresse bestätigt lediglich den Zugriff auf diese E-Mail-Adresse; sie garantiert weder deinen Immatrikulationsstatus noch die Richtigkeit deiner Beiträge.',
      ],
    },
    {
      heading: '3. Art der Informationen und Haftungsbeschränkung',
      paragraphs: [
        'Die vom Dienst bereitgestellten Informationen, etwa Hochschulberichte, sind Referenzmaterial, das mithilfe von KI aus öffentlichen Quellen und offiziellen Materialien der Hochschulen zusammengestellt wurde; Quellen und Informationsstand werden jeweils angegeben. Wichtige Angelegenheiten wie Visa, Studienangelegenheiten und Kosten musst du unbedingt anhand der offiziellen Angaben der zuständigen Stelle überprüfen. Der Dienst übernimmt keine Gewähr für die Richtigkeit und Aktualität der Informationen.',
        'Die Auskunftsfunktion des International Office antwortet ausschließlich im Rahmen der von der jeweiligen Hochschule freigegebenen Materialien und trifft keine Beurteilung deiner persönlichen Situation (etwa ob ein Aufenthaltstitel möglich ist). Die Auskünfte sind weder eine offizielle Entscheidung der Hochschule noch eine Rechtsberatung.',
      ],
    },
    {
      heading: '4. Inhalte der Nutzer',
      paragraphs: [
        'Die Rechte an den von dir hochgeladenen Beiträgen, Kommentaren, Nachrichten und Dateien verbleiben bei dir. Du gestattest dem Dienst, diese Inhalte in dem Umfang zu nutzen, der erforderlich ist, um sie innerhalb des Dienstes anzuzeigen, zu speichern, zu übermitteln und zu übersetzen. Du darfst urheberrechtlich geschützte Werke oder personenbezogene Daten anderer nicht ohne deren Einwilligung hochladen.',
      ],
    },
    {
      heading: '5. Verbotene Handlungen',
      paragraphs: [
        'Die folgenden Handlungen sind verboten. Bei Verstößen können Inhalte gelöscht, die Nutzung eingeschränkt oder das Konto gekündigt werden.',
      ],
      bullets: [
        'Sich als eine andere Person ausgeben, Verifizierungsversuche mit falschen Angaben, Teilen von Konten',
        'Veröffentlichung von Beschimpfungen, Hass, Diskriminierung, Belästigung, obszönen oder rechtswidrigen Inhalten',
        'Werbung, Spam oder wiederholte Veröffentlichungen zu kommerziellen Zwecken (ausgenommen vom Dienst gekennzeichnete Partnerhinweise)',
        'Umgehung der Sicherheitsmaßnahmen des Dienstes oder massenhafte Datenerhebung mit automatisierten Mitteln',
        'Nutzung der Auskunfts- oder Beratungsfunktion des International Office für Falschmeldungen, Scherze oder Drohungen',
      ],
    },
    {
      heading: '6. Null-Toleranz gegenüber anstößigen Inhalten und missbräuchlichen Nutzern',
      paragraphs: [
        'Der Dienst duldet keinerlei anstößige oder unangemessene Inhalte und keine Nutzer, die andere belästigen. Bei der Registrierung musst du diesem Grundsatz zustimmen; er gilt für alle Inhalte, die Mitglieder veröffentlichen, einschließlich Beiträgen, Kommentaren, Chatnachrichten und Profilen. Insbesondere sind die folgenden Inhalte verboten:',
      ],
      bullets: [
        'Spam, Werbung, Betrug oder wiederholte Veröffentlichung gleicher Inhalte',
        'Belästigung, Mobbing, Drohungen und gegen eine bestimmte Person gerichtete Beleidigungen',
        'Hassrede und Diskriminierung aufgrund von Rasse, ethnischer Herkunft, Staatsangehörigkeit, Religion, Geschlecht, sexueller Orientierung, Behinderung oder ähnlichen Merkmalen',
        'Pornografische oder sexuell explizite Inhalte sowie jegliche Inhalte, die Minderjährige sexualisieren',
        'Inhalte, die Gewalt oder Selbstverletzung fördern oder verherrlichen',
        'Handel mit illegalen Waren oder Dienstleistungen sowie sonstige rechtswidrige Inhalte',
        'Veröffentlichung personenbezogener Daten anderer, wie Kontaktdaten, Adressen oder Ausweisdokumente, ohne deren Einwilligung',
      ],
    },
    {
      heading: '7. Melden, Blockieren und Bearbeitungsfrist',
      paragraphs: [
        'Du kannst solche Inhalte oder Nutzer über die Funktion „Melden“ bei Beiträgen, Kommentaren, Chatnachrichten und Mitgliederprofilen melden und ein bestimmtes Mitglied über die Funktion „Blockieren“ blockieren. Nach einer Blockierung seht ihr die Beiträge und Kommentare des anderen nicht mehr, könnt euch keine Nachrichten mehr senden, und bestehende Follows zwischen euch werden entfernt. Du kannst die Blockierung jederzeit unter „Blockierte Mitglieder“ in deinem Profil aufheben.',
        'Eingegangene Meldungen werden innerhalb von 24 Stunden vom Betreiber geprüft. Wird ein Verstoß festgestellt, werden die betreffenden Inhalte gelöscht und die Nutzung des verstoßenden Nutzers eingeschränkt oder sein Konto gesperrt bzw. gekündigt. Falschmeldungen und der Missbrauch der Meldefunktion können ebenfalls zu Nutzungseinschränkungen führen.',
      ],
    },
    {
      heading: '8. Partnerhinweise',
      paragraphs: [
        'Der Dienst kann externe Dienstleistungen, die für das Einleben benötigt werden, etwa SIM-Karten, Versicherungen, Unterkünfte oder Abholdienste, mit der Kennzeichnung „Partner“ vorstellen und für deren Nutzung eine Provision erhalten. Die Kennzeichnung wird stets angebracht, und Partnerhinweise erscheinen nie im Text der vom International Office freigegebenen Auskünfte. Verträge über externe Dienstleistungen und die Verantwortung dafür liegen beim jeweiligen Anbieter.',
      ],
    },
    {
      heading: '9. Externe Videolinks (YouTube)',
      paragraphs: [
        'Auf den Seiten der Hochschulberichte zeigt der Dienst Links zu öffentlichen YouTube-Videos mit Bezug zur jeweiligen Hochschule zusammen mit Titel, Kanalname und Vorschaubild an. Die Videos werden weder im Dienst gespeichert noch innerhalb des Dienstes abgespielt; ein Klick auf einen Link führt dich zu YouTube. Diese Funktion nutzt die YouTube-API-Dienste, und wer diese Funktion nutzt, stimmt damit auch den YouTube-Nutzungsbedingungen zu.',
        `Inhalt und Rechte an jedem Video liegen beim jeweiligen Urheber des Videos, und der Dienst übernimmt keine Gewähr für den Inhalt der Videos. Links zu Videos, die gelöscht oder auf privat gestellt wurden, werden regelmäßig überprüft und entfernt. Wenn der Urheber eines Videos oder eine im Video erscheinende Person die Entfernung eines Links verlangt, entfernen wir ihn unverzüglich. Anfragen: ${LEGAL_CONTACT_PLACEHOLDER}`,
      ],
      links: [{ label: 'YouTube-Nutzungsbedingungen', href: YOUTUBE_TERMS_URL }],
    },
    {
      heading: '10. Änderung und Unterbrechung des Dienstes',
      paragraphs: [
        'Der Dienst kann Funktionen hinzufügen, ändern oder einstellen; wesentliche Änderungen werden vorab angekündigt. Aus unvermeidbaren Gründen wie Naturkatastrophen oder Systemausfällen kann der Dienst vorübergehend unterbrochen werden.',
      ],
    },
    {
      heading: '11. Kündigung',
      paragraphs: [
        'Du kannst dein Konto jederzeit in deinem Profil löschen. Bei der Löschung werden deine personenbezogenen Daten gemäß der Datenschutzerklärung behandelt.',
      ],
    },
    {
      heading: '12. Anwendbares Recht und Streitigkeiten',
      paragraphs: [
        'Diese Nutzungsbedingungen unterliegen dem Recht der Republik Korea; Streitigkeiten werden vor dem nach der koreanischen Zivilprozessordnung zuständigen Gericht verhandelt.',
      ],
    },
    {
      heading: '13. Änderung der Nutzungsbedingungen',
      paragraphs: [
        `Änderungen dieser Nutzungsbedingungen werden 7 Tage vor ihrem Inkrafttreten angekündigt (Änderungen zum Nachteil der Nutzer 30 Tage vorher). Kontakt: ${LEGAL_CONTACT_PLACEHOLDER}`,
      ],
    },
    {
      heading: '14. Sprache',
      paragraphs: ['Diese Nutzungsbedingungen werden in koreanischer Sprache verfasst. Übersetzungen in andere Sprachen werden zur Erleichterung bereitgestellt; weichen eine Übersetzung und das koreanische Original voneinander ab, gilt das koreanische Original.'],
    },
  ],
}
