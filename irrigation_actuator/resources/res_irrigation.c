#include "contiki.h"
#include "coap-engine.h"
#include <string.h>
#include "time.h"
#include "os/dev/leds.h"

/* Log configuration */
#include "sys/log.h"
#define LOG_MODULE "App"
#define LOG_LEVEL LOG_LEVEL_DBG

extern char mote_zone[15];
extern bool zone_assigned;
static bool irrigation_state = 0;

static void res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
static void res_post_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);

RESOURCE(res_irrigation,
         "title=\"Irrigation Actuator: POST/PUT state=ON|OFF&zone=<value>\";if=\"actuator\";rt=\"Control\"",
         res_get_handler,
         res_post_put_handler,
         res_post_put_handler,
         NULL);

static void res_post_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset)
{
  if(request != NULL) {
    LOG_DBG("Request Sent\n");
    LOG_DBG("Actual state: %d\n", irrigation_state);
  }
  size_t len = 0;
  const char *state = NULL;
  int success = 1;

  const char *params = NULL;
  char new_zone[15];
  len = coap_get_post_variable(request, "state", &state);
  LOG_DBG("Len: %d\n", len);
  if(len > 0) {
    printf("State sent - %s", &state);
    if(strncmp(state, "ON", len) == 0) {
      irrigation_state = 1;
      LOG_DBG("Irrigation Started! \n");
      leds_set(LEDS_NUM_TO_MASK(LEDS_GREEN));
    } 
    else if(strncmp(state, "OFF", len) == 0) {
      irrigation_state = 0;
      LOG_DBG("Irrigation Stopped \n");
      leds_set(LEDS_NUM_TO_MASK(LEDS_RED));
    } 
    else
      success = 0;
  } 
  else{
    len = coap_get_post_variable(request, "zone", &params);
    printf("parameter - %s", &params);
    if(len > 0 && len < 15) {
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
      success=1;
    }
    else{
      success=0;
    }
  }

  if(success)
    coap_set_status_code(response, CHANGED_2_04);
  else
    coap_set_status_code(response, BAD_REQUEST_4_00);	
}

static void res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset)
{
  if(request != NULL) 
    LOG_DBG("GET Request Sent\n");

  LOG_DBG("STATE: %d\n", irrigation_state);

  coap_set_header_content_format(response, APPLICATION_JSON);
  snprintf((char *)buffer, COAP_MAX_CHUNK_SIZE, "{\"state\":%d}", irrigation_state);
  coap_set_payload(response, buffer, strlen((char *)buffer));
}
