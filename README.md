# atomix-hackathon
This is an example of Atomix framework presented as a demo to mock the service syncing on change of state. It shows how to build and add distributed services based on three type of Atomix Replicas where one might have multiple copies as services. Another two replicas represents event change publisher and UI.

## Slides

## How to run
1. Run atomix-change-publisher, atomix-view, and atomix-service as seperate spring applications. You can add run as many atomix-services as long as the port in the application.properties file is unoccupied. To mock multi-service situration, you can use a different atomix.service.name in application.properties.
2. open http://localhost:8080/

## Web UI
- Restart: remove dead service nodes, add unregisted service nodes, clear datastore for each running service
- Start: publisher node starts publishing random location event
- Run WebSocket: building connection between UI and BE, node list and event map will be sent as json and refreshed every second

## Change Event
  In this Demo application, we use a simple map structure to mock CUD events.
  every possible change could be randomly from the following combination:  
  - 30 locations(id 0 - 29)
  - 7 sources {"GOOGLE", "FACEBOOK", "GLASSDOOR", "BING", "REPUTATION", "INSTAGRAM", "DELETED"}

     *Note: DELETED is special source type, when the service see location-DELETED event, it removes the location from datastore*
     
## Services
  Each running service application has its unique memberId in the format of serviceName-UUID. For all service applications with the same serviceName, they share the following resource:
  1. DataStore, simple map structure of location-source pair. Naming rule: serviceName-datastore
  2. AtomicLock, to make sure only one replica of the service is processing the change event. Naming rule: serviceName-lock
  On updating datastore, service node has 1/100 chance of failure to test the fault-tolerant feature. 



