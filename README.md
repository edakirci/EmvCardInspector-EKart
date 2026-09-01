![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-REST%20API-brightgreen)
![React](https://img.shields.io/badge/React-Desktop%20UI-blue)
![Electron](https://img.shields.io/badge/Electron-Desktop-47848F)
![Maven](https://img.shields.io/badge/Build-Maven-darkgreen)
![License](https://img.shields.io/badge/Access-Read--Only-success)

# EMV Card Inspector

This project is a **read-only EMV card inspection application** developed for **E-Kart Electronic Card Systems**.

The goal of the project is to inspect contact and contactless payment cards by communicating with them through a PC/SC-compatible smart card reader. The application sends standard read-only APDU commands, parses card responses, validates EMV data, and presents the collected information through a desktop interface.

The system supports both **contact and contactless cards** and automatically selects the appropriate reader interface. Card responses are parsed using the **BER-TLV format**, allowing EMV tags, payment applications, processing options, and card records to be displayed in a structured form.

Sensitive cardholder data is masked before being displayed or exported. The application does not write data to the card or modify its contents.

---

# Features

* Detect PC/SC-compatible smart card readers
* Support contact and contactless card interfaces
* Automatically select the appropriate reader
* Wait for card insertion or presentation
* Send standard read-only APDU commands
* Select PSE for contact cards
* Select PPSE for contactless cards
* Discover payment applications stored on the card
* Select applications using their AID values
* Send GET PROCESSING OPTIONS commands
* Parse AIP and AFL data
* Read EMV records according to AFL entries
* Parse BER-TLV encoded responses
* Display nested TLV structures
* Show raw APDU commands and responses
* Interpret SW1 and SW2 status words
* Measure APDU command execution times
* Identify supported payment schemes
* Mask sensitive cardholder information
* Search parsed EMV tags
* Preview and export inspection results as CSV files
* Provide both command-line and desktop interfaces
* Support light and dark interface themes


<img width="2504" height="1370" alt="image" src="https://github.com/user-attachments/assets/de73d794-1538-4bda-86eb-50ed6e706bcc" />


---

# Supported Payment Schemes

The application includes EMV tag definitions for the following payment schemes:

* Visa
* Mastercard
* American Express
* China UnionPay
* TROY
* Other EMV-compatible applications

Payment schemes are identified using the RID section of the application’s AID.

If an application-specific tag definition cannot be found, the application falls back to the common EMV tag dictionary.

---

# Technologies Used

* Java 21
* Spring Boot
* Java Smart Card I/O
* PC/SC
* React
* TypeScript
* Electron
* Vite
* Maven
* JUnit 5
* JSON
* CSV
* HTML and CSS

---

# System Requirements

The project requires the following components:

* JDK 21
* Maven 3.9 or Maven Wrapper
* Node.js and npm
* Windows operating system
* PC/SC-compatible smart card reader
* Installed reader drivers
* Contact or contactless EMV test card

The project was developed and tested using the **HID OMNIKEY 5422** dual-interface smart card reader.

---

# How the Application Works

The application begins by allowing the user to select a card interface:

1. Contact card
2. Contactless card

After the interface is selected, the system detects available PC/SC readers and automatically chooses the most suitable reader. It then waits for a card and establishes a connection.

For contact cards, the application starts the inspection by sending a `SELECT PSE` command. For contactless cards, it sends a `SELECT PPSE` command.

The general inspection flow is:

1. Detect the card reader
2. Wait for the card
3. Establish a PC/SC connection
4. Select PSE or PPSE
5. Discover payment applications
6. Select each application using its AID
7. Send GET PROCESSING OPTIONS
8. Parse AIP and AFL information
9. Read the records referenced by the AFL
10. Parse the responses as BER-TLV
11. Validate and display EMV data
12. Mask sensitive values
13. Prepare the inspection results for CSV export
14. Close the card connection safely

Only one card inspection can run at a time.

---

# Read-Only Inspection

The application is designed exclusively for inspection and analysis.

It uses standard read-only EMV commands such as:

* `SELECT PSE`
* `SELECT PPSE`
* `SELECT AID`
* `GET PROCESSING OPTIONS`
* `READ RECORD`

The application does not use commands that:

* Change card data
* Update EMV records
* Modify application settings
* Write personal information
* Perform payment transactions

This makes the application suitable for controlled inspection of EMV test cards.

---

# EMV Data Processing

Card responses are parsed according to the **BER-TLV encoding format**.

The parser supports:

* Single-byte and multi-byte tags
* Short and long-form definite lengths
* Primitive TLV objects
* Constructed TLV objects
* Nested TLV structures
* Response validation
* Malformed-data detection

Some of the processed EMV fields include:

* Application Identifier
* Application Label
* Application Preferred Name
* Application Priority Indicator
* Processing Options Data Object List
* Application Interchange Profile
* Application File Locator
* Track 2 Equivalent Data
* Primary Account Number
* Cardholder Name
* Application Expiration Date

Sensitive fields such as PAN, track data, cardholder information, and payment identifiers are marked and masked before being displayed or exported.

---

# APDU Response Information

For each command, the application displays:

* Command name
* Sent APDU command
* Complete raw response
* Response data
* SW1 value
* SW2 value
* Combined status word
* Status description
* Command duration
* Parsing result
* Parsed EMV tags

Successful responses containing EMV data are displayed as a structured TLV tree.

If a command fails or returns no response data, parsing is skipped and the reason is shown to the user.

---

# CSV Report

After a successful inspection, the collected information can be converted into a CSV report.

The CSV report can include:

* Reader information
* Selected card interface
* Detected payment applications
* APDU command summaries
* Status words
* Command execution times
* Parsed EMV tags
* Tag names and values
* Sensitive-data indicators

A preview is displayed before the report is saved. Sensitive values remain masked inside the exported file.

---

# REST API

The Java backend runs locally at:

```text
http://127.0.0.1:8080
```

Available endpoints:

```text
GET  /api/health
POST /api/inspections/contact
POST /api/inspections/contactless
```

The health endpoint checks whether the backend is running.

The inspection endpoints start a contact or contactless inspection and return the technical output as a JSON response.

---

# Project Structure

* `src/main/java/com/emvcardinspector/reader/` → PC/SC reader detection and card connections
* `src/main/java/com/emvcardinspector/apdu/` → APDU command, response, transport, and status-word models
* `src/main/java/com/emvcardinspector/tlv/` → BER-TLV parser and TLV data models
* `src/main/java/com/emvcardinspector/emv/` → EMV commands, parsers, validators, and tag dictionaries
* `src/main/java/com/emvcardinspector/api/` → Spring Boot REST API
* `src/main/java/com/emvcardinspector/report/` → Inspection report and export components
* `src/main/java/com/emvcardinspector/app/` → Application entry points and command-line interface
* `src/test/` → Unit and integration tests
* `desktop/src/` → React and TypeScript desktop interface
* `desktop/electron/` → Electron main and preload processes
* `pom.xml` → Maven dependencies and build configuration

---

# How to Run

## 1. Clone the Repository

```bash
git clone https://github.com/edakirci/EmvCardInspector-EKart.git
cd EmvCardInspector-EKart
```

## 2. Run the Tests

On Windows:

```powershell
.\mvnw.cmd clean test
```

Using a system Maven installation:

```powershell
mvn clean test
```

## 3. Start the Backend

Run the Spring Boot REST API from the project root:

```powershell
.\mvnw.cmd spring-boot:run
```

The backend will be available at:

```text
http://127.0.0.1:8080
```

## 4. Install Desktop Dependencies

Open another terminal:

```powershell
cd desktop
npm install
```

## 5. Start the Desktop Application

```powershell
npm run dev
```

This command starts both the React development server and the Electron desktop window.

## 6. Create the Frontend Production Build

```powershell
npm run build
```

---

# Running the Command-Line Interface

First, compile the Java project:

```powershell
.\mvnw.cmd compile
```

Then run the command-line application:

```powershell
java -cp target\classes com.emvcardinspector.app.Main
```

The command-line interface detects connected PC/SC readers, waits for a card, displays ATR and protocol information, and runs the selected contact or contactless EMV inspection flow.

---

# Safety and Privacy

* The application performs read-only card inspection
* Card contents are never modified
* Sensitive EMV values are masked
* The backend listens only on `127.0.0.1`
* Only one inspection can run at a time
* Card connections are closed after each inspection
* The application is intended for authorized test and development environments

---
