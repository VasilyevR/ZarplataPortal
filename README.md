# Zarplata Portal

Zarplata Portal is a corporate web application designed for internal use within an Active Directory environment. It provides employees with access to their salary information and allows authorized personnel to generate purchase orders from Excel invoices.

## Business Features

*   **Salary Dashboard**: Employees can view their salary history parsed in real-time from personal Excel files. Data is grouped by month and year with interactive drill-down details.
*   **On-the-fly Parsing**: Salary data is not stored in the database but parsed directly from a secure network share when a user accesses the portal. Results are cached in the user session for performance.
*   **Purchase Order Generation**: Authorized users (e.g., managers) can upload multiple Excel invoices. The system automatically parses them, aggregates items by supplier (identified by cell background color), and generates separate order files for each supplier.
*   **Funds Receipt**: Authorized users (Funds Receivers) can upload bank statements to automatically find and match payments in managers' salary files. The system searches for exact amount matches within the last 3 months and allows users to apply the payment date to the corresponding salary record.
*   **Manager Overlook**: Users with elevated roles can view salary data for any employee by selecting them from a managed list.

## Technical Features

*   **Single Sign-On (SSO)**: Seamless authentication using Windows credentials (NTLM/Kerberos) via Waffle. No manual login required for domain users.
*   **Role-Based Access Control (RBAC)**: Dynamic mapping of application roles to Active Directory groups stored in the database.
*   **Dynamic Column Mapping**: Administrators can configure which Excel columns are visible to users and which columns should be used for total amount calculations via database settings.
*   **Internationalization (i18n)**: Full support for English and Russian languages.
*   **Excel Processing**: Advanced parsing of `.xlsx` files using Apache POI, including real-time cell background color mapping to HTML styles.
*   **Secure Deployment**: Runs as a Windows Service under a specific service account with limited privileges.

## Technology Stack

*   **Java 17**
*   **Spring Boot 3.3.3** (Web, Security, Data JPA, Thymeleaf)
*   **Waffle 3.5.0**: For Windows Authentication (SSO).
*   **Apache POI 5.2.5**: For processing Excel files (.xlsx).
*   **MySQL**: Database for storing user data, invoices, and configuration.
*   **Thymeleaf**: Server-side template engine.
*   **Maven**: Build tool.

## Environment Setup

### 1. Active Directory (AD)

*   **Service Account**: Create a dedicated user in AD (e.g., `YOUR_DOMAIN\ServiceAccountName`) to run the application service.
    *   **SPN Registration**: Register Service Principal Names (SPN) for this account to enable Kerberos/Negotiate authentication.
        ```cmd
        setspn -A HTTP/portal.domain.local YOUR_DOMAIN\ServiceAccountName
        setspn -A HTTP/server.domain.local YOUR_DOMAIN\ServiceAccountName
        ```
    *   **Encryption**: In the account properties ("Account" tab), check "This account supports Kerberos AES 128/256 bit encryption".
*   **Groups**: Create AD groups for access control (e.g., `TargetADGroup` for order generators).

### 2. DNS

*   Create an **A Record** for the server IP (e.g., `server.domain.local` -> `192.168.x.x`).
*   Create a **CNAME Record** (Alias) for the user-friendly URL (e.g., `portal.domain.local` -> `server.domain.local`).

### 3. Group Policy (GPO) & Browser Configuration

To ensure SSO works correctly (especially for Chrome/Edge), browsers must trust the intranet site.

*   **Intranet Zone**: Add `https://portal.domain.local` and `https://server.domain.local` to the "Local Intranet" zone.
    *   *User Configuration -> Administrative Templates -> Windows Components -> Internet Explorer -> Internet Control Panel -> Security Page -> Site to Zone Assignment List*. (Value `1` for Intranet).
*   **Logon Options**: Ensure "Automatic logon only in Intranet zone" is selected.
*   **Chrome/Edge Policies** (using ADMX templates):
    *   `AuthServerAllowlist`: `*.portal.domain.local,portal.domain.local`
    *   `AuthNegotiateDelegateAllowlist`: `*.portal.domain.local,portal.domain.local`

### 4. Database (MySQL)

*   Create a database and user.
*   Run the DDL scripts to create tables: `user`, `invoice`, `supplier_setting`, `invoice_parse_setting`, `role_mapping`, `global_setting`.
*   Populate initial configuration data (Domain name, Role mappings, Supplier colors).

### 5. Deployment as a Windows Service (WinSW)

The application is deployed as an executable JAR wrapped in a Windows Service using **WinSW**.

1.  **Download WinSW**: Rename `WinSW-x64.exe` to `ZarplataPortal.exe`.
2.  **Configuration (`ZarplataPortal.xml`)**:
    ```xml
    <service>
      <id>ZarplataPortal</id>
      <name>Zarplata Portal Service</name>
      <executable>java</executable>
      <arguments>-jar "C:\Path\To\App\zarplata-portal.jar"</arguments>
      <serviceaccount>
        <domain>YOUR_DOMAIN</domain>
        <user>ServiceAccountName</user>
        <password>YOUR_PASSWORD</password>
        <allowservicelogontop>true</allowservicelogontop>
      </serviceaccount>
      <env name="SSL_KEY_STORE_PATH" value="C:\Path\To\App\keystore.p12"/>
      <!-- Other environment variables -->
    </service>
    ```
3.  **Install & Start**:
    ```cmd
    ZarplataPortal.exe install
    ZarplataPortal.exe start
    ```

## Configuration

Application settings are managed via `application.properties` and Environment Variables (passed via WinSW).

*   `server.port`: 443 (HTTPS)
*   `waffle.sso.protocols`: `NTLM` (or `Negotiate;NTLM`)
*   `app.share.folder.path`: Path to the network folder with salary Excel files.

## License

This project is licensed under the terms of the **GNU General Public License v3.0 (GPLv3)**.
