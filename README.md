# VanillaHud (Fabric 1.21.11)

Right Shift oeffnet ein Modul-Menue im Vanilla-Stil (Item-Icons, Standard-Buttons).
"HUD anordnen" = Module per Maus verschieben. Einstellungen: config/vanillahud.json

## Bauen
1. JDK 21 installieren
2. gradle.properties: fabric_version auf die passende Fabric-API-Version fuer 1.21.11 setzen
3. `gradle wrapper` (einmalig, wenn gradlew fehlt), dann `./gradlew build`
4. Jar liegt in build/libs/vanillahud-1.0.0.jar -> in den mods-Ordner (mit Fabric API)

Hinweis: Das Projekt wurde ohne Internet geschrieben und NICHT kompiliert. Fehler bei
Methodennamen (1.21.9+ hat Input-/Rendering-API geaendert) kommen ggf. beim ersten Build.
