# Opening CBDVentures in an IDE

The project is plain **Java 25 + Spring Boot 4.1.2 + Maven**, with a **vanilla JavaScript** front end.
There is no Node/npm step, no TypeScript build, no bundler — the browser loads the files in
`src/main/resources/static` directly. So the only tooling you need is a JDK, the bundled Maven
wrapper, and an editor.

---

## 1. The one hard requirement: JDK 25

`pom.xml` sets `<java.version>25</java.version>`. Anything older will fail to compile.

Check what you have:

```bash
java -version        # must report 25.x
```

If it does not:

```bash
bash tools/setup-jdk.sh          # downloads Temurin JDK 25 (Linux/macOS sandbox script)
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
```

On Windows or a normal desktop, install **Eclipse Temurin JDK 25** from
<https://adoptium.net> and point your IDE at it instead of running that script.

> You do **not** need Maven installed. `./mvnw` (or `mvnw.cmd` on Windows) downloads the matching
> Maven version on first use. On Linux/macOS make sure the script is executable:
> `chmod +x mvnw`.

---

## 2. Recommended IDE: IntelliJ IDEA

**Use IntelliJ IDEA.** For a Spring Boot project it is the least friction: it understands the Maven
wrapper, the `application.yml`, the JPA entities and the REST controllers out of the box.

| Edition | Verdict |
|---|---|
| **Ultimate** | Best choice. Spring-specific tooling: Spring Boot run configurations, bean and endpoint navigation, injected-language support in YAML/SQL. **Free with a JetBrains student licence** (verify with your Richfield e-mail). |
| **Community (free)** | Perfectly adequate for this project — it is a plain Java/Maven app. You lose the Spring inspectors, but the project builds and runs the same way (run `CbdventuresApplication`, or use the Maven `spring-boot:run` goal). |

Use a **current release (2025.2 or newer)** so JDK 25 is fully understood; older builds may
underline newer language features or refuse to load the SDK.

### Setup

1. **File → Open** → select the folder that contains `pom.xml` (`cbdventures-dark/`, not its parent).
   Accept "Open as Project" when IntelliJ offers to import the Maven model.
2. **File → Project Structure → Project** → **SDK: 25**, **Language level: 25**.
   If 25 is not listed: *Add SDK → Download JDK → Temurin 25*.
3. **Settings → Build, Execution, Deployment → Build Tools → Maven** → confirm
   *Use Maven wrapper* is selected (it should be detected automatically).
4. Let the import finish (it downloads dependencies once), then run.

### Running from the IDE

* Open `src/main/java/za/ac/richfield/cbdventures/CbdventuresApplication.java` → click the green
  ▶ in the gutter → **Run**. That is the whole app; the front end is served from the same port.
* To run on another port, add a run configuration with **VM options**: `-Dserver.port=8081`
  (or an environment variable `PORT=8081` — `application.yml` reads `${PORT:8080}`).
* Tests: right-click `src/test/java` → **Run 'All tests'** (76 tests in this build).

### Handy while marking

* **H2 console:** with the app running, open <http://localhost:8080/h2-console/> —
  JDBC URL `jdbc:h2:file:./data/cbdventures`, user `sa`, empty password.
  (Run it from the project root so the relative `./data` path resolves.)
* **Database pane (Ultimate):** add an H2 data source with the same URL to browse the 12 tables.
* **Live CSS/JS edits:** the static files are served from the build output. While running with
  `spring-boot:run` or from the IDE, edits to `src/main/resources/static` are picked up on a
  **hard refresh** (Ctrl/Cmd + Shift + R). Editing inside a packaged jar needs a rebuild.

---

## 3. Alternative: VS Code

Lightweight and fine if you already live in it.

1. Install the **Extension Pack for Java** (Microsoft) and the **Spring Boot Extension Pack** (VMware).
2. **File → Open Folder** → the folder with `pom.xml`.
3. `Ctrl+Shift+P` → **Java: Configure Java Runtime** → select/install **JDK 25**.
4. Run/debug from the *Spring Boot Dashboard* panel, or the ▶ above `main` in `CbdventuresApplication`.

Eclipse works too (**File → Import → Existing Maven Project**), but Spring support needs extra
plugins, so it is not the best use of your time before a demo.

---

## 4. Gotchas that cost people marks

| Gotcha | What to do |
|---|---|
| **Wrong JDK** — `release version 25 not supported` | Set the SDK to 25 in the IDE; on the command line check `JAVA_HOME`. |
| **`./mvnw: Permission denied`** | `chmod +x mvnw` — ZIP copies and some file transfers drop the executable bit. |
| **`mvnw.cmd` on Windows** | Use `mvnw.cmd spring-boot:run`, not `./mvnw`. |
| **Port already in use** | Something else holds 8080 → run with `PORT=8081` or stop the other process. |
| **Running light and dark modes together** | Both use `./data/cbdventures.mv.db` and H2 locks the file. Give the second build its own port **and** database: `PORT=8081 ./mvnw spring-boot:run -Dspring-boot.run.arguments=--spring.datasource.url=jdbc:h2:file:./data/dark` |
| **"My CSS change did nothing"** | Hard refresh; Spring serves static files with `no-cache` but the browser may still hold the module graph. |
| **Data looks wrong before a demo** | Stop the app, delete the `data/` folder, start again — the pilot zone re-seeds (26 live listings, 45 reviews, 12 events). |
| **First build seems to hang** | It is downloading Maven + dependencies. Needs internet the first time; afterwards `-o` (offline) works. |
