# RHIES SHR Registry docker image building
1. Copy your new .war file in the same directory with the Dockerfile,
2. Rename your war file **rhies-shr-server-1.0.0.war** 
3. Build the Hapi Fhir server docker image by running the command  **docker build -t  gtsl/rhies-shr:latest .**  while in the openMRS docker folder
4. Push your image to the docker hub by running the command **docker push gtsl/rhies-shr:latest** 
Please do not forget to change the image version