#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "contiki.h"
#include "coap-engine.h"
#include "coap-blocking-api.h"
#include "random.h"
#include "node-id.h"
#include "os/dev/leds.h"

/* Log configuration */
#include "coap-log.h"
#include "sys/log.h"
#define LOG_MODULE "App"
#define LOG_LEVEL LOG_LEVEL_DBG

#define SERVER "coap://[fd00::1]:5683"
#define SERVER_REGISTRATION "/registration"
#define SERVER_LOOKUP "/lookup"

#define INTERVAL (30 * CLOCK_SECOND)

extern coap_resource_t res_temperature;
extern coap_resource_t res_lamp;
extern bool lamp_state;

int temperature_value = 15;
bool registered = false;
int min_temperature = 0;
int max_temperature = 60;

char mote_zone[15];
bool zone_assigned=false;

PROCESS(node_process, "node");
AUTOSTART_PROCESSES(&node_process);

static struct etimer timer;
static struct etimer reg_timer;

void client_chunk_handler(coap_message_t *response)
{
  const uint8_t *chunk;

  if(response == NULL) {
    LOG_INFO("Request timed out\n");
    return;
  }
	
  int len = coap_get_payload(response, &chunk);
  if (len>0){
    registered = true;	
    LOG_DBG("%s \n", (char *)chunk);
  }  
}

PROCESS_THREAD(node_process, ev, data){

  static coap_endpoint_t server_ep;
  static coap_message_t request[1];
  PROCESS_BEGIN(); 
	
  etimer_set(&reg_timer, 20);
  
  coap_activate_resource(&res_temperature, "temperature");
  coap_activate_resource(&res_lamp, "lamp");
  
  coap_endpoint_parse(SERVER, strlen(SERVER), &server_ep);
  
  while(!registered){
    PROCESS_WAIT_EVENT();
    if(ev == PROCESS_EVENT_TIMER && data == &reg_timer){
      coap_init_message(request, COAP_TYPE_CON, COAP_POST, 0);
      coap_set_header_uri_path(request, (const char *)&SERVER_REGISTRATION);
      const char msg[] = "Registration...";
      printf("%s\n", msg);
      coap_set_payload(request, (uint8_t *)msg, sizeof(msg)-1);
      LOG_INFO("Sending Registration Request\n");
      COAP_BLOCKING_REQUEST(&server_ep, request, client_chunk_handler);
      etimer_reset(&reg_timer);
    }
  }
  
  etimer_set(&timer, INTERVAL);
  while(1) {
    PROCESS_WAIT_EVENT();		
    if(ev == PROCESS_EVENT_TIMER && data == &timer){	
      res_temperature.trigger();
      etimer_reset(&timer);
    }
  }

  PROCESS_END();
}
