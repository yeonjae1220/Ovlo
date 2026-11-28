import type { LegalDocument } from '../legalContent'
import { GOOGLE_PRIVACY_URL, LEGAL_CONTACT } from '../legalConstants'

/** 개인정보처리방침 독일어 번역. 정본은 legalContent.ts 의 한국어(ko). 원문을 바꾸면 이 파일도 같이 고치고 version 을 맞춘다. */
export const PRIVACY_DE: LegalDocument = {
  version: 'v4',
  updatedAt: '2026-11-28',
  title: 'Datenschutzerklärung',
  intro:
    'Ovlo (nachfolgend „Dienst“) legt gemäß dem koreanischen „Gesetz zum Schutz personenbezogener Daten“ (Personal Information Protection Act) die folgende Datenschutzerklärung fest, um die personenbezogenen Daten der Nutzer zu schützen und damit verbundene Anliegen zügig zu bearbeiten.',
  sections: [
    {
      heading: '1. Erhobene personenbezogene Daten',
      paragraphs: ['Der Dienst erhebt bei der Registrierung und Nutzung die folgenden Daten.'],
      bullets: [
        'Pflichtangaben: E-Mail-Adresse, Passwort (nur als Hash gespeichert), Spitzname, Name, Heimatstadt, Heimathochschule, Studiengang, Studienabschluss und Studienjahr',
        'Freiwillige Angaben: Profilfoto, Selbstbeschreibung, Geburtsdatum, Kontaktdaten, Sprachkenntnisse, Auslandsstudienerfahrung',
        'Bei Anmeldung über Google: E-Mail-Adresse des Google-Kontos, Name, Google-Nutzerkennung',
        'Bei Anmeldung über Apple: Apple-Nutzerkennung, die von Apple mitgeteilte E-Mail-Adresse (bei der Option „E-Mail-Adresse verbergen“ die Weiterleitungsadresse von Apple), der vom Nutzer angegebene Name',
        'Bei der Verifizierung der Hochschul-E-Mail-Adresse: verifizierte Hochschul-E-Mail-Adresse, Zeitpunkt der Verifizierung, verifizierte Hochschule',
        'Bei Nutzung der Auskünfte und Beratung des International Office der Hochschule: Fragen und automatische Antworten, in den Antworten zitierte Mitteilungen, Bewertungen der Antworten, der bei einer Beratungsanfrage zugestimmte Wortlaut samt Version, in der Beratungsgruppe ausgetauschte Nachrichten und deren Übersetzungen',
        'Bei Nutzung des Gebrauchtmarkts: Verkaufsanzeigen (Titel, Beschreibung, Preis, Gebiet, Hochschule, Übergabeort, mögliche Übergabetage), Produktfotos, Handels-Chats, Meldegründe',
        'Bei Nutzung des Kalenders: persönliche Termine (Titel, Beschreibung, Ort, Zeit), Inhalt von Anträgen auf Verwaltung des Hochschulkalenders (Abteilung, Kontakt-E-Mail, Nachricht)',
        'Bei Meldungen und Sperrungen: gemeldetes Objekt samt Grund und Beschreibung, gesperrte Mitglieder',
        'Bei Nutzung der Feedback-Funktion: Inhalt des Feedbacks, falls angegeben die Antwort-E-Mail-Adresse, App-Version und Browserinformationen',
        'Bei der Nutzung des Dienstes entstehende Daten: Beiträge, Kommentare, Chat-Nachrichten, Anhänge, Zugriffsprotokolle (IP-Adresse, Zugriffszeitpunkt, Anfragepfad, Mitgliedsnummer), Geräte- und Browserinformationen',
        'Cookies: Refresh-Token (httpOnly) zur Aufrechterhaltung der Anmeldung, Einstellung der Anzeigesprache',
      ],
    },
    {
      heading: '2. Zwecke der Erhebung und Nutzung personenbezogener Daten',
      paragraphs: ['Die erhobenen personenbezogenen Daten werden ausschließlich für die folgenden Zwecke verwendet.'],
      bullets: [
        'Identifizierung der Mitglieder, Anmeldung, Identitätsprüfung (Verifizierung der Hochschul-E-Mail-Adresse)',
        'Bereitstellung und Betrieb von Community-Funktionen wie Boards, Chat, Gebrauchtmarkt und Kalender',
        'Automatische Auskünfte auf Grundlage der Mitteilungen des International Office der Hochschule, Vermittlung von Beratungen mit dem Personal des International Office, Hilfe zur Nutzung des Dienstes',
        'Übersetzung von Mitteilungen, Chat-Nachrichten und Gebrauchtmarkt-Anzeigen, Bereitstellung von Informationen zu Hochschulen und Austauschprogrammen, Anpassung der Anzeigesprache',
        'Verhinderung missbräuchlicher Nutzung, Bearbeitung von Meldungen, Reaktion auf Sicherheitsvorfälle, Aufbewahrung von Zugriffsprotokollen',
        'Beantwortung von Anfragen und Übermittlung von Mitteilungen',
      ],
    },
    {
      heading: '3. Funktionen, die KI nutzen',
      paragraphs: [
        'Die folgenden Funktionen des Dienstes erzeugen Antworten oder Übersetzungen mithilfe eines externen KI-Dienstes. An den externen KI-Dienst senden wir nur den Text der Frage bzw. Nachricht sowie die für die Antwort nötigen Mitteilungen und Hilfeartikel; identifizierende Daten wie Name, E-Mail-Adresse oder Mitgliedsnummer senden wir nicht. Was du selbst in den Text schreibst, wird jedoch unverändert übermittelt; wir empfehlen daher, keine sensiblen personenbezogenen Daten anzugeben.',
      ],
      bullets: [
        'Automatische Auskünfte des International Office: Studierende, die ihre Hochschul-E-Mail-Adresse verifiziert haben, erhalten Antworten auf Grundlage der Mitteilungen, die das International Office ihrer Hochschule freigegeben hat. Fragen und Antworten werden gespeichert, um die Antwortqualität zu prüfen und sie in einer Beratung fortzuführen (Aufbewahrungsdauer in Abschnitt 5).',
        'Vermittlung einer Beratung mit dem International Office: Nur wenn du auf einem gesonderten Bildschirm zustimmst, wird der Gesprächsinhalt an das Personal des International Office deiner Hochschule weitergegeben. Das Personal sieht die Fragen und automatischen Antworten des ausgewählten Gesprächs (höchstens 20 Wechsel), die verwendete Sprache, ob die Hochschul-E-Mail-Adresse verifiziert ist, den in der Beratungsgruppe angezeigten Spitznamen und das Profilfoto sowie die späteren Nachrichten in der Beratungsgruppe; die E-Mail-Benachrichtigung über eine neue Beratung enthält zusätzlich die erste Frage. E-Mail-Adresse und Name werden nicht weitergegeben. Nachrichten in der Beratungsgruppe werden jeweils in die Sprache des anderen übersetzt.',
        'Hilfe zum Dienst: Fragt ein angemeldetes Mitglied, wie der Dienst zu nutzen ist, antwortet der Dienst anhand der von ihm verfassten Hilfeartikel. Der Fragetext wird nur während der Erstellung der Antwort verarbeitet und nicht gespeichert.',
        'Übersetzung: Mitteilungen des International Office und Gebrauchtmarkt-Anzeigen werden in die Sprache des Nutzers übersetzt.',
      ],
    },
    {
      heading: '4. Was das Personal des International Office sehen kann',
      paragraphs: [
        'Das Personal des International Office besteht aus Mitgliedern des Dienstes, die vom International Office der jeweiligen Hochschule eingeladen wurden. Es kann nur die Beratungen sehen, die bei der eigenen Hochschule eingehen, und hat nach dem Ausscheiden aus dieser Aufgabe keinen Zugriff mehr auf die Beratungsgruppen. Die Beratungsliste, die das Personal herunterladen kann, enthält Status, Zeitpunkte, Sprache und erste Frage der Beratung, nicht aber die E-Mail-Adresse oder den Namen der Studierenden.',
      ],
    },
    {
      heading: '5. Aufbewahrungs- und Nutzungsdauer',
      paragraphs: [
        'Personenbezogene Daten werden unverzüglich vernichtet, sobald der Zweck ihrer Erhebung und Nutzung erreicht ist. Die folgenden Daten werden jedoch für den angegebenen Zeitraum aufbewahrt.',
      ],
      bullets: [
        'Mitgliedsdaten: bis zur Kontolöschung. Mit der Kontolöschung werden die Kontodaten sofort unkenntlich gemacht und die hochgeladenen Dateien (Profilfoto, Anhänge) gelöscht. Beiträge, Kommentare, Chat-Nachrichten und Gebrauchtmarkt-Anzeigen werden nicht gelöscht, sondern für andere Nutzer ausgeblendet. Ein gelöschtes Konto kann nicht wiederhergestellt werden.',
        'Aufzeichnungen zur Verifizierung der Hochschul-E-Mail-Adresse: laufen 1 Jahr nach dem Verifizierungsdatum ab und werden bei der Kontolöschung gelöscht.',
        'Fragen, Antworten und Beratungen des International Office: Der Text wird 1 Jahr nach dem Tag der Frage (bei Beratungen nach dem Tag, an dem die Beratung eröffnet wurde) gelöscht, bei Kontolöschung sofort. Nutzungszeitpunkt und Bearbeitungsergebnis bleiben für Statistikzwecke erhalten.',
        'Fragen an die Hilfe zum Dienst: werden nicht gespeichert. Nur eine Zählung zur Begrenzung der Nutzungshäufigkeit wird höchstens einen Tag lang vorgehalten.',
        'Persönliche Kalendertermine und Anträge auf Verwaltung des Hochschulkalenders: werden bei der Kontolöschung gelöscht.',
        'Meldeaufzeichnungen: 1 Jahr nach dem Tag, an dem die Bearbeitung abgeschlossen wurde (bei Gebrauchtmarkt-Meldungen nach dem Eingangstag), werden das meldende Mitglied, das gemeldete Objekt und der Grund gelöscht; für Statistikzwecke bleiben nur Meldungsart, Bearbeitungsergebnis und Datum erhalten.',
        'Sperraufzeichnungen: bis zur Aufhebung der Sperre oder bis eines der beiden Mitglieder sein Konto löscht',
        'Zugriffsprotokolle (IP-Adresse, Zugriffszeitpunkt, Anfragepfad, Mitgliedsnummer): 3 Monate (koreanisches „Gesetz zum Schutz des Kommunikationsgeheimnisses“)',
        'IP-Adressen und E-Mail-Adressen zur Begrenzung von Anmelde-, Registrierungs- und Suchversuchen: werden innerhalb von 10 Minuten automatisch gelöscht',
        'Datenbank-Backups: Zur Wiederherstellung nach Störungen werden Backups gesondert aufbewahrt; gelöschte Daten können daher in den Backups verbleiben, bis diese nacheinander gelöscht werden. Backups werden nur zur Wiederherstellung verwendet.',
      ],
    },
    {
      heading: '6. Auftragsverarbeitung',
      paragraphs: [
        'Der Dienst verkauft die personenbezogenen Daten der Nutzer nicht und gibt sie nicht an Dritte weiter. Für den Betrieb des Dienstes werden die folgenden Aufgaben im Auftrag vergeben.',
      ],
      bullets: [
        'AI3 Inc. (BizRouter): Weiterleitung von KI-Anfragen — automatische Auskünfte des International Office, Übersetzung von Beratungsnachrichten, Mitteilungen und Gebrauchtmarkt-Anzeigen, Antworten der Hilfe zum Dienst',
        'Anthropic PBC: Erzeugung von KI-Antworten und Übersetzungen (Verarbeitung über BizRouter)',
        'Google LLC: Authentifizierung bei der Anmeldung über Google, E-Mail-Versand über Gmail (Verifizierungscodes für die Hochschul-E-Mail-Adresse, Benachrichtigungen an das Personal des International Office)',
        'Apple Inc.: Authentifizierung bei der Anmeldung über Apple und Trennung der Apple-Verknüpfung bei der Kontolöschung',
        'Cloudflare, Inc.: Weiterleitung der Verbindungen zum Dienst (Übertragungssicherheit) und Zustellung von Anfrage-E-Mails',
      ],
    },
    {
      heading: '7. Übermittlung personenbezogener Daten ins Ausland',
      paragraphs: [
        'Zur Erledigung der beauftragten Aufgaben werden personenbezogene Daten wie folgt ins Ausland übermittelt. Jede Übermittlung erfolgt jedes Mal, wenn der Nutzer die betreffende Funktion verwendet, über ein verschlüsseltes Netzwerk (HTTPS). Die Aufbewahrungsdauer beim Empfänger richtet sich nach der Datenschutzerklärung und den Nutzungsbedingungen des jeweiligen Unternehmens.',
      ],
      bullets: [
        'Anthropic PBC (USA, privacy@anthropic.com): Fragen an das International Office und frühere Wechsel (höchstens 20), als Grundlage der Antworten dienende Mitteilungen und Hilfeartikel, Text der zu übersetzenden Nachrichten, Mitteilungen und Gebrauchtmarkt-Anzeigen — Erzeugung von KI-Antworten und Übersetzungen',
        'Google LLC (USA): Kontodaten bei der Anmeldung über Google, Empfängeradresse und Inhalt der versendeten E-Mails (Verifizierungscode, erste Frage einer Beratung) — Anmeldeauthentifizierung und E-Mail-Versand',
        'Apple Inc. (USA): Bestätigungsdaten der Anmeldung über Apple und das bei der Kontolöschung zur Trennung verwendete Token — Anmeldeauthentifizierung und Trennung der Verknüpfung',
        'Cloudflare, Inc. (USA): beim Zugriff auf den Dienst übertragene Anfragen (einschließlich IP-Adresse) und Anfrage-E-Mails — Weiterleitung von Verbindungen und Zustellung von E-Mails',
        'Ablehnung: Wenn du die KI-Funktionen und die Anmeldung über Google oder Apple nicht nutzt, werden die entsprechenden Daten nicht übermittelt. Die Weiterleitung von Verbindungen über Cloudflare ist für die Nutzung des Dienstes unerlässlich; lehnst du sie ab, kannst du den Dienst nicht nutzen.',
      ],
    },
    {
      heading: '8. Rechte der Nutzer',
      paragraphs: [
        'Du kannst jederzeit Auskunft über deine personenbezogenen Daten, deren Berichtigung oder Löschung oder die Einschränkung ihrer Verarbeitung verlangen; du kannst sie direkt in deinem Profil ändern oder dich unter den unten angegebenen Kontaktdaten an uns wenden. Du kannst dein Konto in deinem Profil löschen und jederzeit in der Beratungsgruppe die Beendigung einer Beratung des International Office verlangen.',
      ],
    },
    {
      heading: '9. Cookies und lokaler Speicher',
      paragraphs: [
        'Zur Aufrechterhaltung der Anmeldung speichern wir ein Refresh-Token in einem httpOnly-Cookie, und die Einstellung der Anzeigesprache legen wir in einem Cookie und im Browserspeicher ab. Cookies zu Werbe- oder Tracking-Zwecken verwenden wir nicht. Du kannst Cookies in deinen Browsereinstellungen ablehnen; in diesem Fall bleibst du jedoch möglicherweise nicht angemeldet.',
      ],
    },
    {
      heading: '10. Externe Videolinks und YouTube-API-Dienste',
      paragraphs: [
        'Der Dienst nutzt die YouTube-API-Dienste, um die Liste der YouTube-Videolinks zu erstellen, die auf den Seiten der Hochschulberichte angezeigt werden. Dabei übermittelt der Dienst keine personenbezogenen Daten der Nutzer an YouTube oder Google und erhebt auch keine Daten über die Nutzer über YouTube.',
        'Die Vorschaubilder der Videos lädt dein Browser jedoch direkt von den Bildservern von YouTube; wenn du eine Seite mit einer Videoliste öffnest, können daher deine IP-Adresse und Browserinformationen an Google übermittelt werden. Für die Verarbeitung von Daten, nachdem du über einen Link zu YouTube gewechselt bist, gilt die Datenschutzerklärung von Google.',
      ],
      links: [{ label: 'Datenschutzerklärung von Google', href: GOOGLE_PRIVACY_URL }],
    },
    {
      heading: '11. Maßnahmen zur Gewährleistung der Sicherheit',
      paragraphs: [
        'Passwörter werden ausschließlich als nicht entschlüsselbare Hashes gespeichert, Anmelde-Token werden als Hashes gespeichert, und die Übertragung wird mit HTTPS verschlüsselt. Der Zugriff von Administratoren wird durch eine gesonderte Authentifizierung und die Protokollierung der Zugriffe kontrolliert.',
        'In Beiträgen und Chats hochgeladene Fotos werden im Original gespeichert, sodass Informationen im Foto, etwa der Aufnahmeort, erhalten bleiben können. Wenn dein Standort nicht erkennbar sein soll, entferne die Standortdaten des Fotos vor dem Hochladen. Fotos von Gebrauchtmarkt-Artikeln speichert der Dienst neu ab und entfernt dabei solche Informationen.',
      ],
    },
    {
      heading: '12. Datenschutzbeauftragter und Kontakt',
      paragraphs: [
        `Datenschutzbeauftragter: der Betreiber des Dienstes · Kontakt: ${LEGAL_CONTACT}`,
        'Für Meldungen und Beratung zu Verletzungen des Schutzes personenbezogener Daten kannst du dich an das koreanische Meldezentrum für Verletzungen personenbezogener Daten (Personal Information Infringement Report Center; privacy.kisa.or.kr, 118) und an den Schlichtungsausschuss für Streitigkeiten über personenbezogene Daten (Personal Information Dispute Mediation Committee; kopico.go.kr, 1833-6972) wenden.',
      ],
    },
    {
      heading: '13. Änderungen dieser Datenschutzerklärung',
      paragraphs: [
        'Änderungen dieser Datenschutzerklärung werden ab 7 Tage vor ihrem Inkrafttreten durch eine Mitteilung im Dienst bekannt gegeben. Ändern sich die erhobenen Daten, die Zwecke oder die Aufbewahrungsdauer zum Nachteil der Nutzer, erfolgt die Mitteilung 30 Tage vorher.',
      ],
    },
    {
      heading: '14. Sprache',
      paragraphs: ['Diese Datenschutzerklärung wird in koreanischer Sprache verfasst. Übersetzungen in andere Sprachen werden zur Erleichterung bereitgestellt; weichen eine Übersetzung und das koreanische Original voneinander ab, gilt das koreanische Original.'],
    },
  ],
}
