import dht
import machine
import time
import network
import simple as mqtt
import _thread

# Wi-Fi Details
ssid = 'Wokwi-GUEST'
password = ''

# MQTT Details
mqtt_server = '192.168.36.1'
client_id = 'pico_park'
topic_temp = 'home/sensors/temperature'
topic_pir = 'home/sensors/pir'
topic_mq4 = 'home/sensors/dangerous gases'
topic_pulse = 'home/sensors/pulse'
topic_led = 'home/actuators/led'

# Sensor GPIO Pins
DHT_PIN = 22
MUX_PIN = 35
PIR_PIN = 34
LED_PIN = 19
SELECT_PIN = 23

# Sensor Initialization
# Initialize the DHT22 sensor
dht_sensor = dht.DHT22(machine.Pin(DHT_PIN))

# Initialize the MQ sensor (as an ADC input)
# Initialize the MUX control pin
mux_select = machine.Pin(SELECT_PIN, machine.Pin.OUT)
mux = machine.ADC(machine.Pin(MUX_PIN))
mux.atten(machine.ADC.ATTN_11DB)  # Adjust ADC range if needed

# Initialize the PIR sensor (as a digital input)
pir_sensor = machine.Pin(PIR_PIN, machine.Pin.IN)

# Initialize the LED (as a digital output)
led = machine.Pin(LED_PIN, machine.Pin.OUT)

# Wi-Fi Connection
print("Connecting to Wi-Fi...")
wlan = network.WLAN(network.STA_IF)
wlan.active(True)
wlan.connect(ssid, password)

while not wlan.isconnected():
    print("Connecting...")
    time.sleep(1)

print("Connected to Wi-Fi:", wlan.ifconfig())

# Initialize MQTT Client
client = mqtt.MQTTClient(client_id, mqtt_server, port=1883)

# Sensor Reading Functions
def read_dht_sensor():
    dht_sensor.measure()
    return dht_sensor.temperature(), dht_sensor.humidity()

def read_mq_sensor():
    mux_select.value(1)
    time.sleep(0.1)  # Allow settling time
    raw_value = mux.read_u16()
    return (raw_value / 65535) * 10000  # Convert ADC value to PPM approximation

def read_pulse_sensor():
    mux_select.value(0)
    time.sleep(0.1)  # Allow settling time
    raw_value = mux.read_u16()
    return (raw_value / 65535) * 200

def read_pir_sensor():
    return pir_sensor.value()

# MQTT Callback Function
def message_callback(topic, msg):
    if topic.decode() == topic_led:
        if msg.decode().lower() == "on":
            led.value(1)  # Turn on LED
            print("LED turned ON")
        elif msg.decode().lower() == "off":
            led.value(0)  # Turn off LED
            print("LED turned OFF")

# Thread Function for Subscribing
def mqtt_subscribe_thread():
    print("Starting MQTT subscribe thread...")
    client.set_callback(message_callback)
    client.connect()
    client.subscribe(topic_led)
    print(f"Subscribed to {topic_led}")

    while True:
        client.wait_msg()  # Wait for incoming messages

# Main Function
def main():
    print("Starting main module...")

    # Start MQTT subscription thread
    _thread.start_new_thread(mqtt_subscribe_thread, ())

    # Main loop for publishing sensor data
    while True:
        temp, _ = read_dht_sensor()
        ppm = read_mq_sensor()
        pulse = read_pulse_sensor()
        pir_state = read_pir_sensor()

        temp = round(temp, 2)
        ppm = round(ppm, 2)

        client.publish(topic_temp, str(temp))

        if ppm > 3000:
            client.publish(topic_mq4, str(ppm))

        client.publish(topic_pulse, str(pulse))

        if pir_state:
            client.publish(topic_pir, "Motion detected")

        print(f"Temp: {temp}C, PPM: {ppm}, Pulse: {pulse}, PIR: {'Motion detected' if pir_state else 'No motion'}")

        time.sleep(2)

# Run Main Function
if __name__ == "__main__":
    main()