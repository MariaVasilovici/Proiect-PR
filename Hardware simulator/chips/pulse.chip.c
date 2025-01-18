// Wokwi Custom Chip - For docs and examples see:
// https://docs.wokwi.com/chips-api/getting-started
//
// SPDX-License-Identifier: MIT
// Copyright 2023 ProCoding Whitehat JR

#include "wokwi-api.h"
#include <stdio.h>
#include <stdlib.h>

typedef struct {
  pin_t pin;
  int pulse;
} chip_data_t;


void chip_timer_callback(void *data) {
  chip_data_t *chip_data = (chip_data_t*)data;
  int pulse = attr_read(chip_data->pulse);
  
  // Pulse is between 40 and 200
  // Simulate converting TDS value to voltage
  float volts = pulse * 5.0 / 200;
  
  // Send the correct voltage on the out pin
  pin_dac_write(chip_data->pin, volts);
}

void chip_init() {
  chip_data_t *chip_data = (chip_data_t*)malloc(sizeof(chip_data_t));
  chip_data->pulse = attr_init("pulse", 40); 
  chip_data->pin = pin_init("OUT", ANALOG);

  // TODO: Initialize the chip, set up IO pins, create timers, etc.
  
  const timer_config_t config = {
    .callback = chip_timer_callback,
    .user_data = chip_data,
  };

  timer_t timer_id = timer_init(&config);
  timer_start(timer_id, 1000, true); 

  printf("Hello from custom chip!\n");
}
