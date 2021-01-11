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
#define UPPER_THRESHOLD		90
#define PERIODIC_HANDLER_INTERVAL 10

//static int humidity_value = 15;
extern int humidity_value;
static int counter = 0;

extern char mote_zone[15];
extern bool zone_assigned;
extern char actuator_ip[100];
extern bool actuator_ip_registered;
extern int min_humidity;
extern int max_humidity;


static void res_post_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
static void res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
static void res_event_handler(void);

EVENT_RESOURCE(res_humidity,
   "title=\"Soil Humidity: POST/PUT zone:<value>&actuators<value>&min_humidity<value>>&max_humidity<value>\";obs;if=\"sensor\";rt=\"Control\"",
   res_get_handler,
   res_post_put_handler,
   res_post_put_handler,
   NULL,
   res_event_handler);

static void res_event_handler(void)
{
  counter ++;
  humidity_value = (rand() %  (UPPER_THRESHOLD - LOWER_THRESHOLD + 1)) + LOWER_THRESHOLD;
  coap_notify_observers(&res_humidity);
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

  len = coap_get_post_variable(request, "actuators", &params);
  if(len > 0) {
    LOG_DBG("Actuators update");
    sprintf(actuator_ip, "%s", params);
    if (strcmp(actuator_ip, "None")==0){
      strcpy(actuator_ip, "");
      actuator_ip_registered = false;
    }
    else
      actuator_ip_registered = true;

    created = true;
    LOG_DBG("Actuator: %s \n", actuator_ip);
  }

  len = coap_get_post_variable(request, "min_humidity", &params);
  if(len > 0) {
    LOG_DBG("Min humidity update");
    //sprintf(actuator_ip, "%s", params);
    min_humidity = atoi(params);
    created = true;
    LOG_DBG("Min humidity updated\n");
  }

  len = coap_get_post_variable(request, "max_humidity", &params);
  if(len > 0) {
    LOG_DBG("Max humidity update");
    //sprintf(actuator_ip, "%s", params);
    max_humidity = atoi(params);
    created = true;
    LOG_DBG("Max humidity updated\n");
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
  coap_set_header_content_format(response, APPLICATION_JSON);
  snprintf((char *)buffer, COAP_MAX_CHUNK_SIZE, "{\"humidity\":\"%d\"}", humidity_value);
  coap_set_payload(response, buffer, strlen((char *)buffer));   
}
