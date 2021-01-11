#include "contiki.h"

#include <stdio.h>
#include <string.h>
#include "time.h"
#include "coap-engine.h"
#include "coap-observe.h"

/* Log configuration */
#include "sys/log.h"
#define LOG_MODULE "App"
#define LOG_LEVEL LOG_LEVEL_DBG

#define LOWER_THRESHOLD		10
#define UPPER_THRESHOLD		20
#define PERIODIC_HANDLER_INTERVAL 10


static int counter = 0;

extern int temperature_value;
extern char mote_zone[15];
extern bool zone_assigned;
extern int min_temperature;
extern int max_temperature;
extern bool lamp_state;


static void res_post_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
static void res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
static void res_event_handler(void);

EVENT_RESOURCE(res_temperature,
   "title=\"Soil Temperature: POST/PUT zone=<value>&min_temperature=<value>>&max_temperature=<value>\";obs;if=\"sensor\";rt=\"Control\"",
   res_get_handler,
   res_post_put_handler,
   res_post_put_handler,
   NULL,
   res_event_handler);

static void res_event_handler(void)
{
  counter ++;
  temperature_value = (rand() %  (UPPER_THRESHOLD - LOWER_THRESHOLD + 1)) + LOWER_THRESHOLD;
  coap_notify_observers(&res_temperature);
}

static void res_post_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset){

  const char *params = NULL;
  size_t len = 0;
  char new_zone[15];
  bool created = false;

  len = coap_get_post_variable(request, "zone", &params);
  if(len > 0 && len < 15) {
    LOG_DBG("Zone update");
    sprintf(new_zone, "%s", params);
    printf("New zone: %s\n", new_zone);
    if(strcmp(new_zone, "None")==0){
      strcpy(mote_zone, "");
      zone_assigned = false;
    }
    else{
      strcpy(mote_zone, new_zone);
      zone_assigned = true;
    }
    created = true;
  }

  len = coap_get_post_variable(request, "min_temperature", &params);
  if(len > 0) {
    LOG_DBG("Min temperature update");
    min_temperature = atoi(params);
    created = true;
    LOG_DBG("Min temperature updated\n");
  }

  len = coap_get_post_variable(request, "max_temperature", &params);
  if(len > 0) {
    LOG_DBG("Max temperature update");
    max_temperature = atoi(params);
    created = true;
    LOG_DBG("Max temperature updated\n");
  }
  if (created)
    coap_set_status_code(response, CREATED_2_01);
  else
    coap_set_status_code(response, BAD_REQUEST_4_00);
}

static void res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset)
{
  if(request != NULL) {
    LOG_DBG("Observing Handler Number %d\n", counter);
  }

  if(temperature_value > max_temperature){
    if(!lamp_state){
      LOG_DBG("Lamp stop.\n");
      lamp_state = 0;
    }
  } else if(temperature_value < min_temperature){
    if(lamp_state){
      LOG_DBG("Lamp start.\n");
      lamp_state = 1;
    }
  }

  coap_set_header_content_format(response, APPLICATION_JSON);
  snprintf((char *)buffer, COAP_MAX_CHUNK_SIZE, "{\"temperature\":\"%d\"}", temperature_value);
  coap_set_payload(response, buffer, strlen((char *)buffer));   
}
