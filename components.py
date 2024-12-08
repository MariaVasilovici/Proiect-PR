import random
import time
import threading
from paho.mqtt import client as mqtt_client
import queue

# MQTT Configuration
mqtt_server = '192.168.68.245'
client_id = 'pico_park'

# Global variable to control the simulation
running = True

# Queue to communicate with the main thread
input_queue = queue.Queue()

def on_connect(client, userdata, flags, rc):
    print("Conectat la broker MQTT, cod de rezultat: " + str(rc))
    if rc == 0:
        print("Conexiune reusita")
        # Subscribe to the LED actuator topic on successful connection
        client.subscribe("home/actuators/led")
        print("Subscribed to topic: home/actuators/led")
    else:
        print(f"Conexiune esuata, cod de eroare: {rc}")

def on_message(client, userdata, msg):
    """
    Callback to process received MQTT messages.
    """
    if msg.topic == "home/actuators/led":
        # Process LED actuator messages
        message = msg.payload.decode()
        if message.lower() == "on":
            print("LED turned ON")
        elif message.lower() == "off":
            print("LED turned OFF")
        else:
            print(f"Received unknown LED command: {message}")
    else:
        print(f"Received message on {msg.topic}: {msg.payload.decode()}")

# Create an MQTT client and configure callbacks
client = mqtt_client.Client(mqtt_client.CallbackAPIVersion.VERSION1, client_id)
client.on_connect = on_connect
client.on_message = on_message

# Function to handle MQTT loop in a separate thread
def mqtt_loop():
    try:
        client.connect(mqtt_server, port=1883, keepalive=60)
        print("Conectare la broker MQTT...")
        client.loop_forever()  # This will block until disconnected
    except Exception as e:
        print("Eroare de conectare:", e)

# Start MQTT loop in a separate thread
mqtt_thread = threading.Thread(target=mqtt_loop, daemon=True)
mqtt_thread.start()

def read_keyboard_input():
    """
    Reads input from the keyboard without blocking the main loop.
    Stops the simulation if the user types 'stop'.
    """
    global running
    while running:
        user_input = input()  # Waits for user input
        input_queue.put(user_input)  # Put input into the queue for the main thread to process

def simulate_temperature(prev_temp, max_change=1.5, min_temp=15, max_temp=35):
    """
    Simulates the next hour's temperature based on the previous temperature,
    with constraints to keep it realistic.
    """
    new_temp = prev_temp + random.uniform(-max_change, max_change)
    return round(max(min_temp, min(new_temp, max_temp)), 2)

def simulate_pulse(prev_pulse, max_change=1, min_pulse=60, max_pulse=100):
    """
    Simulates a pulse reading based on the previous value,
    with constraints to keep it within a realistic range.
    """
    new_pulse = prev_pulse + random.randint(-max_change, max_change)
    return max(min_pulse, min(new_pulse, max_pulse))

# Start a thread to handle user input
input_thread = threading.Thread(target=read_keyboard_input, daemon=True)
input_thread.start()

print("Starting hourly temperature simulation. Type 'stop' to end.")

try:
    current_temp = 20  # Starting temperature (in °C)
    current_pulse = 75 # Starting pulse
    i = 1

    while running:
        # Check for user input
        if not input_queue.empty():
            user_input = input_queue.get()
            if user_input.strip().lower() == "stop":
                running = False
                print("Stopping simulation...")
            elif user_input.strip().lower() == "pir":
                topic = "home/sensors/pir"
                client.publish(topic, "MOVEMENT DETECTED")
                print("Published PIR movement detected.")
            elif user_input.strip().lower() == "gas":
                topic = "home/sensors/dangerous gases"
                client.publish(topic, "DANGEROUS GAS DETECTED")
                print("Published dangerous gas detected.")
        
        # Publish data
        if i == 3:
            client.publish("home/sensors/temperature", current_temp)
            i = 0
        i += 1
        client.publish("home/sensors/pulse", current_pulse)

        # Simulate the next data
        if i == 1:
            current_temp = simulate_temperature(current_temp)
        current_pulse = simulate_pulse(current_pulse)

        time.sleep(1)

except KeyboardInterrupt:
    print("\nSimulation interrupted.")
finally:
    running = False
    input_thread.join()
    print("Simulation stopped.")
