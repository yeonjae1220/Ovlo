import type { LegalDocument } from '../legalContent'
import { GOOGLE_PRIVACY_URL, LEGAL_CONTACT_PLACEHOLDER } from '../legalConstants'

/** 개인정보처리방침 독일어 번역. 정본은 legalContent.ts 의 한국어(ko). 원문을 바꾸면 이 파일도 같이 고치고 version 을 맞춘다. */
export const PRIVACY_DE: LegalDocument = {
  version: 'v3',
  updatedAt: '2026-11-25',
  title: 'Datenschutzerklärung',
  intro:
    'Ovlo (nachfolgend „Dienst“) legt gemäß dem koreanischen „Gesetz zum Schutz personenbezogener Daten“ (Personal Information Protection Act) die folgende Datenschutzerklärung fest, um die personenbezogenen Daten der Nutzer zu schützen und damit verbundene Anliegen zügig zu bearbeiten.',
  sections: [
    {
      heading: '1. Erhobene personenbezogene Daten',
      paragraphs: ['Der Dienst erhebt bei der Registrierung und der Nutzung die folgenden Daten.'],
      bullets: [
        'Pflichtangaben: E-Mail-Adresse, Passwort (nur als Hash gespeichert), Spitzname, Name, Heimatstadt, Heimathochschule, Studiengang, Studienabschluss und Studienjahr',
        'Freiwillige Angaben: Profilfoto, Selbstbeschreibung, Geburtsdatum, Kontaktdaten, Sprachkenntnisse, Auslandsstudienerfahrung',
        'Bei der Anmeldung über ein soziales Netzwerk (Google): E-Mail-Adresse des Google-Kontos, Name, Google-Nutzerkennung',
        'Bei der Verifizierung der Hochschul-E-Mail-Adresse: die verifizierte Hochschul-E-Mail-Adresse, der Zeitpunkt der Verifizierung und die verifizierte Hochschule',
        'Bei der Nutzung des Dienstes entstehende Daten: Beiträge, Kommentare, Chatnachrichten und angehängte Dateien, Zugriffsprotokolle (IP-Adresse, Zugriffszeitpunkt, angefragter Pfad), Geräte- und Browserinformationen',
        'Cookies: Refresh-Token zur Aufrechterhaltung der Anmeldung (httpOnly), Einstellung der Anzeigesprache',
      ],
    },
    {
      heading: '2. Zwecke der Erhebung und Nutzung personenbezogener Daten',
      paragraphs: ['Die erhobenen personenbezogenen Daten werden ausschließlich für die folgenden Zwecke genutzt.'],
      bullets: [
        'Identifizierung von Mitgliedern, Anmeldung, Identitätsbestätigung (Verifizierung der Hochschul-E-Mail-Adresse)',
        'Bereitstellung und Betrieb von Community-Funktionen wie Boards und Chat',
        'Bereitstellung von Informationen zu Hochschulen und zum Studierendenaustausch, Anpassung der Anzeigesprache',
        'Verhinderung missbräuchlicher Nutzung, Reaktion auf Sicherheitsvorfälle, Aufbewahrung von Zugriffsprotokollen',
        'Beantwortung von Anfragen und Übermittlung von Mitteilungen',
      ],
    },
    {
      heading: '3. Auskunfts- und Beratungsfunktion des International Office der Hochschulen (in Vorbereitung)',
      paragraphs: [
        'Der Dienst bereitet eine Funktion vor, die internationalen Studierenden Auskünfte auf Grundlage von Materialien erteilt, die vom International Office der jeweiligen Hochschule freigegeben wurden, und die Inhalte von Anfragen nur dann an die zuständigen Mitarbeitenden des International Office dieser Hochschule weiterleitet, wenn du eingewilligt hast.',
        'Sobald diese Funktion freigeschaltet ist, gilt: ① Die von dir eingegebenen Fragen und die automatischen Antworten werden zur Messung der Servicequalität protokolliert; ② der Text deiner Frage kann zur Beantwortung im Auftrag durch einen externen KI-Dienstleister verarbeitet werden (identifizierende Daten wie Name oder E-Mail-Adresse werden nicht übermittelt); ③ welche Inhalte in welchem Umfang an die zuständigen Mitarbeitenden weitergeleitet werden, wird zum Zeitpunkt der Weiterleitung auf einem gesonderten Einwilligungsbildschirm erläutert. Vor der Freischaltung der Funktion aktualisieren wir diese Datenschutzerklärung und nennen die konkreten Auftragsverarbeiter und Aufbewahrungsfristen.',
      ],
    },
    {
      heading: '4. Aufbewahrungs- und Nutzungsdauer',
      paragraphs: [
        'Personenbezogene Daten werden unverzüglich vernichtet, sobald der Zweck ihrer Erhebung und Nutzung erreicht ist. Die folgenden Daten werden jedoch für die jeweils angegebene Dauer aufbewahrt.',
      ],
      bullets: [
        'Mitgliedsdaten: bis zur Löschung des Kontos. Mit der Löschung des Kontos werden die Kontodaten sofort gelöscht oder so verarbeitet, dass sie keine Identifizierung mehr ermöglichen, und deine Beiträge und Kommentare werden nicht mehr öffentlich angezeigt.',
        'Zugriffsprotokolle (IP-Adresse, Zugriffszeitpunkt): 3 Monate (koreanisches „Gesetz zum Schutz des Kommunikationsgeheimnisses“)',
        'Aufzeichnungen zur Verifizierung der Hochschul-E-Mail-Adresse: verfallen 1 Jahr nach dem Tag der Verifizierung und werden bei der Löschung des Kontos gelöscht.',
        'Aufzeichnungen über missbräuchliche Nutzung: 1 Jahr, um eine Wiederholung zu verhindern',
      ],
    },
    {
      heading: '5. Weitergabe an Dritte und Auftragsverarbeitung',
      paragraphs: [
        'Der Dienst verkauft die personenbezogenen Daten der Nutzer nicht und gibt sie nicht an Dritte weiter. Für den Betrieb des Dienstes werden die folgenden Aufgaben im Auftrag vergeben.',
      ],
      bullets: [
        'Google LLC: Authentifizierung bei der Anmeldung über ein soziales Netzwerk (Google OAuth)',
        'E-Mail-Versanddienstleister: Versand der Codes zur Verifizierung der Hochschul-E-Mail-Adresse',
        'Externer KI-Dienstleister: Verarbeitung des Fragetextes, sobald die Auskunftsfunktion des International Office (Abschnitt 3) freigeschaltet ist. Der Auftragsverarbeiter wird vor dem Start der Funktion in dieser Datenschutzerklärung genannt.',
      ],
    },
    {
      heading: '6. Rechte der Nutzer',
      paragraphs: [
        'Du kannst jederzeit Auskunft über deine personenbezogenen Daten, deren Berichtigung oder Löschung oder die Einschränkung ihrer Verarbeitung verlangen; du kannst sie direkt in deinem Profil ändern oder dich unter den unten angegebenen Kontaktdaten an uns wenden. Du kannst dein Konto in deinem Profil löschen.',
      ],
    },
    {
      heading: '7. Cookies und lokaler Speicher',
      paragraphs: [
        'Zur Aufrechterhaltung der Anmeldung speichern wir ein Refresh-Token in einem httpOnly-Cookie, und die Einstellung der Anzeigesprache legen wir in einem Cookie und im Browserspeicher ab. Cookies zu Werbe- oder Tracking-Zwecken verwenden wir nicht. Du kannst Cookies in deinen Browsereinstellungen ablehnen; in diesem Fall bleibst du jedoch möglicherweise nicht angemeldet.',
      ],
    },
    {
      heading: '8. Externe Videolinks und YouTube-API-Dienste',
      paragraphs: [
        'Der Dienst nutzt die YouTube-API-Dienste, um die Liste der YouTube-Videolinks zu erstellen, die auf den Seiten der Hochschulberichte angezeigt werden. Dabei übermittelt der Dienst keine personenbezogenen Daten der Nutzer an YouTube oder Google und erhebt auch keine Daten über die Nutzer über YouTube.',
        'Die Vorschaubilder der Videos lädt dein Browser jedoch direkt von den Bildservern von YouTube; wenn du eine Seite mit einer Videoliste öffnest, können daher deine IP-Adresse und Browserinformationen an Google übermittelt werden. Für die Verarbeitung von Daten, nachdem du über einen Link zu YouTube gewechselt bist, gilt die Datenschutzerklärung von Google.',
      ],
      links: [{ label: 'Datenschutzerklärung von Google', href: GOOGLE_PRIVACY_URL }],
    },
    {
      heading: '9. Maßnahmen zur Gewährleistung der Sicherheit',
      paragraphs: [
        'Passwörter werden ausschließlich als nicht entschlüsselbare Hashes gespeichert, Anmelde-Token werden als Hashes gespeichert, und die Übertragung wird mit HTTPS verschlüsselt. Der Zugriff von Administratoren wird durch eine gesonderte Authentifizierung und die Protokollierung der Zugriffe kontrolliert.',
      ],
    },
    {
      heading: '10. Datenschutzbeauftragter und Kontakt',
      paragraphs: [
        `Datenschutzbeauftragter: der Betreiber des Dienstes · Kontakt: ${LEGAL_CONTACT_PLACEHOLDER}`,
        'Für Meldungen und Beratung zu Verletzungen des Schutzes personenbezogener Daten kannst du dich an das koreanische Meldezentrum für Verletzungen personenbezogener Daten (Personal Information Infringement Report Center; privacy.kisa.or.kr, 118) und an den Schlichtungsausschuss für Streitigkeiten über personenbezogene Daten (Personal Information Dispute Mediation Committee; kopico.go.kr, 1833-6972) wenden.',
      ],
    },
    {
      heading: '11. Änderungen dieser Datenschutzerklärung',
      paragraphs: [
        'Änderungen dieser Datenschutzerklärung werden ab 7 Tage vor ihrem Inkrafttreten durch eine Mitteilung im Dienst bekannt gegeben. Ändern sich die erhobenen Daten, die Zwecke oder die Aufbewahrungsdauer zum Nachteil der Nutzer, erfolgt die Mitteilung 30 Tage vorher.',
      ],
    },
    {
      heading: '12. Sprache',
      paragraphs: ['Diese Datenschutzerklärung wird in koreanischer Sprache verfasst. Übersetzungen in andere Sprachen werden zur Erleichterung bereitgestellt; weichen eine Übersetzung und das koreanische Original voneinander ab, gilt das koreanische Original.'],
    },
  ],
}
