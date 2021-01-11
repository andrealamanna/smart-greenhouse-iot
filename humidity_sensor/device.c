#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "contiki.h"
#include "coap-engine.h"
#include "coap-blocking-api.h"
#include "random.h"
#include "node-id.h"

/* Log configuration */
#include "coap-log.h"
#include "sys/log.h"
#define LOG_MODULE "App"
#define LOG_LEVEL LOG_LEVEL_DBG

#define SERVER "coap://[fd00::1]:5683"
#define SERVER_REGISTRATION "/registration"
#define SERVER_LOOKUP "/lookup"

#define INTERVAL (30 * CLOCK_SECOND)

extern coap_resource_t res_humidity;

int humidity_value = 15;
bool registered = false;
char actuator_ip[100];
bool actuator_ip_registered = false;
int min_humidity = 0;
int max_humidity = 0;

char mote_zone[15];
bool zone_assigned=false;
bool actuator_error=false;

PROCESS(node_process, "node");
PROCESS(actuator_from_lookup, "lookup");
PROCESS(actuator_change_state, "actuator_state");
AUTOSTART_PROCESSES(&node_process);

static struct etimer timer;
static struct etimer reg_timer;

void sensor_chunk_handler(coap_message_t *response){
  const uint8_t *chunk;
  if(response == NULL) {
    LOG_INFO("Request timed out\n");
    actuator_error = true;
/*
    static coap_endpoint_t server_ep;
    static coap_message_t request[1];
    coap_endpoint_parse(SERVER, strlen(SERVER), &server_ep);
    coap_init_message(request, COAP_TYPE_CON, COAP_POST, 0);
    coap_set_header_uri_path(request, (const char *)&SERVER_LOOKUP);
    //coap_set_payload(response, buffer, strlen((char *)buffer));   
    //const char msg[] ="error_zone="+mote_zone;
    char* msg;
    msg = malloc(strlen(mote_zone)+11);
    strcpy(msg, "error_zone");
    strcat(msg, mote_zone);
    printf("%s\n", msg);
    coap_set_payload(request, (uint8_t *)msg, sizeof(msg)-1);
    LOG_INFO("Sending Actuator Error\n");
    COAP_BLOCKING_REQUEST(&server_ep, request, lookup_chunk_handler);
*/
    return;
  }

  int len = coap_get_payload(response, &chunk);
  if (len>0){
    actuator_error=false;	
    LOG_DBG("Actuator response: %s \n", (char *)chunk);
  }  
}

void lookup_chunk_handler(coap_message_t *response){
  const uint8_t *chunk;
  if(response == NULL) {
    LOG_INFO("Request timed out\n");
    return;
  }
  
  int len = coap_get_payload(response, &chunk);

  if (len>0){
    //actuator_ip_registered = true;
    //sprintf(actuator_ip,"%s",(char *)chunk);
    LOG_DBG("Lookup response: %s \n", (char *)chunk); 
  }  
}

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
  
  coap_activate_resource(&res_humidity, "humidity");
  
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
    
    if(zone_assigned && !actuator_ip_registered){
      process_start( &actuator_from_lookup, data);
      //PROCESS_WAIT_EVENT_UNTIL(ev == PROCESS_EVENT_EXITED);
      //LOG_DBG("IP: %s\n", actuator_ip);
    }
      
    PROCESS_WAIT_EVENT();		
    if(ev == PROCESS_EVENT_TIMER && data == &timer){	
      res_humidity.trigger();
      if(actuator_ip_registered){
        process_start( &actuator_change_state, data);
        //PROCESS_WAIT_EVENT_UNTIL(ev == PROCESS_EVENT_EXITED);
        //LOG_DBG("State Changed");
      }
      etimer_reset(&timer);
    }
  }

  PROCESS_END();
}

PROCESS_THREAD(actuator_from_lookup, ev, data)
{

  static coap_endpoint_t server_lookup;
  static coap_message_t req_lookup[1];
  
  PROCESS_BEGIN();	
  
  static char query[100];  
  sprintf(query, "?zone=%s", (const char*)mote_zone);  
  LOG_DBG("Sending get query to lookup interface:\n%s\n", query);
  coap_endpoint_parse(SERVER, strlen(SERVER), &server_lookup);
  coap_init_message(req_lookup, COAP_TYPE_CON, COAP_GET, 0);
  //coap_set_header_uri_path(req_lookup, "lookup");
  coap_set_header_uri_path(req_lookup, (const char *)&SERVER_LOOKUP);
  coap_set_header_uri_query(req_lookup, query);
  LOG_DBG("Sending Lookup Request\n");
  COAP_BLOCKING_REQUEST(&server_lookup, req_lookup, lookup_chunk_handler);
  LOG_DBG("Lookup terminated");
  PROCESS_END();

}

PROCESS_THREAD(actuator_change_state, ev, data)
{

  static coap_endpoint_t server_actuator;
  static coap_message_t req_actuator[1];
  PROCESS_BEGIN();	
    
  static char query[100];  
  if (humidity_value < min_humidity)
    sprintf(query, "state=%s", (const char*)"ON");
  if (humidity_value > max_humidity)
    sprintf(query, "state=%s", (const char*)"OFF");
  
  LOG_DBG("Sending change state to actuator with query: %s\n", query);
  coap_endpoint_parse(actuator_ip, strlen(actuator_ip), &server_actuator);
  coap_init_message(req_actuator, COAP_TYPE_CON, COAP_PUT, 0);
  //coap_set_header_uri_path(req_actuator, "irrigation_actuator");
  coap_set_header_uri_path(req_actuator, "irrigation");
  coap_set_payload(req_actuator, query, strlen(query));
  LOG_DBG("Sending Actuator Request\n");
  LOG_DBG("actuator: %s\n",actuator_ip);
  COAP_BLOCKING_REQUEST(&server_actuator, req_actuator, sensor_chunk_handler);
  if(actuator_error){
    static coap_endpoint_t server;
    static coap_message_t request[1];
    coap_endpoint_parse(SERVER, strlen(SERVER), &server);
    coap_init_message(request, COAP_TYPE_CON, COAP_POST, 0);
    coap_set_header_uri_path(request, (const char *)&SERVER_LOOKUP);
    char* msg;
    msg = malloc(strlen(mote_zone)+11);
    strcpy(msg, "error_zone");
    strcat(msg, mote_zone);
    printf("%s\n", msg);
    coap_set_payload(request, (uint8_t *)msg, sizeof(msg)-1);
    LOG_INFO("Sending Actuator Error\n");
    COAP_BLOCKING_REQUEST(&server, request, lookup_chunk_handler);    
  }

  PROCESS_END();
}

