Structura Fișierelor

    BlueAlert/
    Conține toate fișierele necesare pentru aplicația mobilă. Utilizatorul va trebui să utilizeze Android Studio pentru a importa acest proiect și a-l instala pe un dispozitiv mobil.

    Hardware simulator/
    Directorul dedicat simulării hardware, care include:
        diagram.json: Diagrama hardware creată în Wokwi.
        src/: Codul pentru microcontroller:
            main.py: Cod principal pentru rularea simulării.
            simple.py: Cod care trebuie încărcat pe microcontroller.
        chips/: Contine implementările personalizate pentru senzorii MQ-4 și Puls:
            Fiecare senzor include codul în C, fișierul JSON pentru configurare și versiunea compilată în WebAssembly (.wasm).

    components.py
    Un script Python pentru generarea datelor simulate, utilizat înainte de a dezvolta simularea hardware în Wokwi.

    Raport detaliat.pdf
    Documentația proiectului, ce include Introducere, Arhitectură, Implementare, etc.



Instrucțiuni de Instalare și Utilizare

1. Configurarea Brokerului MQTT

    Instalați Mosquitto de pe pagina oficială Mosquitto.
    Configurați brokerul MQTT prin editarea fișierului mosquitto.conf pentru a permite conexiuni externe.
    Porniți brokerul folosind comanda:

	mosquitto -v -c "C:\Program Files\mosquitto\mosquitto.conf"

Notă: Asigurați-vă că IP-ul din fișierul MainActivity.java (pentru aplicația mobilă) și src/main.py (pentru simularea hardware) corespunde cu cel al brokerului MQTT.


2. Instalarea și Rularea Aplicației Mobile

    Descărcați și instalați Android Studio.
    Deschideți Android Studio și importați directorul BlueAlert/.
    Conectați un dispozitiv Android (sau utilizați un emulator).
    Rulați aplicația pe telefon pentru a verifica funcționalitatea.


3. Simularea Hardware

Pregătirea Microcontrollerului

    Conectați microcontrollerul utilizând mpremote.
    Încărcați fișierul simple.py pe placă cu comanda:

py -m mpremote connect port:rfc2217://localhost:4000 fs cp src/simple.py :simple.py

Rulați simularea hardware folosind:

py -m mpremote connect port:rfc2217://localhost:4000 run src/main.py
