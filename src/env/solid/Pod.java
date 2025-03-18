package solid;

import cartago.Artifact;
import cartago.OPERATION;
import cartago.OpFeedbackParam;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;


/**
 * A CArtAgO artifact that agent can use to interact with LDP containers in a Solid pod.
 */
public class Pod extends Artifact {

    private String podURL; // the location of the Solid pod 

  /**
   * Method called by CArtAgO to initialize the artifact. 
   *
   * @param podURL The location of a Solid pod
   */
    public void init(String podURL) {
        this.podURL = podURL;
        log("Pod artifact initialized for: " + this.podURL);
    }

  /**
   * CArtAgO operation for creating a Linked Data Platform container in the Solid pod
   *
   * @param containerName The name of the container to be created
   * 
   */
    @OPERATION
    public void createContainer(String containerName) {
        log("1. Implement the method createContainer()");
        String base = this.podURL;
        if (!base.endsWith("/")) {
            base = base + "/";
        }
        String containerURL = base + containerName + "/";
        this.log("Attempting to create container at: " + containerURL);

        URL url;
        HttpURLConnection conn;
        int responseCode;
        try {
            // Check if container exists (HEAD request)
            url = new URL(containerURL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestMethod("HEAD");
            conn.connect();
            responseCode = conn.getResponseCode();
            conn.disconnect();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                this.log("Container already exists at: " + containerURL);
                return;
            }
        } catch (IOException e) {
            this.log("Container does not exist, proceeding to create it: " + e.getMessage());
        }

        try {
            // Create the container with a PUT request.
            url = new URL(containerURL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestMethod("PUT");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "text/turtle");
            // Indicate that we are creating an LDP container.
            conn.setRequestProperty("Link", "<http://www.w3.org/ns/ldp#personal-data>; rel=\"type\"");
            conn.getOutputStream().write(new byte[0]);
            conn.getOutputStream().flush();
            conn.getOutputStream().close();
            responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_CREATED && responseCode != HttpURLConnection.HTTP_OK) {
                this.log("Failed to create container. Response code: " + responseCode);
            } else {
                this.log("Container created successfully at: " + containerURL);
            }
            conn.disconnect();
        } catch (IOException e) {
            this.log("Error creating container: " + e.getMessage());
        }
    }

  /**
   * CArtAgO operation for publishing data within a .txt file in a Linked Data Platform container of the Solid pod
   * 
   * @param containerName The name of the container where the .txt file resource will be created
   * @param fileName The name of the .txt file resource to be created in the container
   * @param data An array of Object data that will be stored in the .txt file
   */
    @OPERATION
    public void publishData(String containerName, String fileName, Object[] data) {
        log("2. Implement the method publishData()");
        String base = this.podURL;
        if (!base.endsWith("/")) {
            base = base + "/";
        }
        String containerURL = base + containerName + "/";
        String resourceURL = containerURL + fileName;
        String dataString = createStringFromArray(data);

        try {
            URL url = new URL(resourceURL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestMethod("PUT");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "text/plain");
            conn.getOutputStream().write(dataString.getBytes(StandardCharsets.UTF_8));
            conn.getOutputStream().flush();
            conn.getOutputStream().close();
            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_CREATED && responseCode != HttpURLConnection.HTTP_OK) {
                this.log("Failed to publish data. Response code: " + responseCode);
            } else {
                this.log("Data published successfully to: " + resourceURL);
            }
            conn.disconnect();
        } catch (IOException e) {
            this.log("Error publishing data: " + e.getMessage());
        }
    }

  /**
   * CArtAgO operation for reading data of a .txt file in a Linked Data Platform container of the Solid pod
   * 
   * @param containerName The name of the container where the .txt file resource is located
   * @param fileName The name of the .txt file resource that holds the data to be read
   * @param data An array whose elements are the data read from the .txt file
   */
    @OPERATION
    public void readData(String containerName, String fileName, OpFeedbackParam<Object[]> data) {
        data.set(readData(containerName, fileName));
    }

  /**
   * Method for reading data of a .txt file in a Linked Data Platform container of the Solid pod
   * 
   * @param containerName The name of the container where the .txt file resource is located
   * @param fileName The name of the .txt file resource that holds the data to be read
   * @return An array whose elements are the data read from the .txt file
   */
    public Object[] readData(String containerName, String fileName) {
        log("3. Implement the method readData(). Currently, the method returns mock data");

        this.log("Attempting to read data from container: " + containerName + ", file: " + fileName);
        String base = this.podURL;
        if (!base.endsWith("/")) {
            base = base + "/";
        }
        String containerURL = base + containerName + "/";
        String resourceURL = containerURL + fileName;

        try {
            URL url = new URL(resourceURL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "text/plain");
            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                this.log("Failed to read data. Response code: " + responseCode);
                conn.disconnect();
                return new Object[0];
            } else {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder responseBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBuilder.append(line).append("\n");
                }
                reader.close();
                conn.disconnect();
                String responseString = responseBuilder.toString();
                this.log("Data read successfully from: " + resourceURL);
                return createArrayFromString(responseString);
            }
        } catch (IOException e) {
            this.log("Error reading data: " + e.getMessage());
            return new Object[0];
        }

    }

  /**
   * Method that converts an array of Object instances to a string, 
   * e.g. the array ["one", 2, true] is converted to the string "one\n2\ntrue\n"
   *
   * @param array The array to be converted to a string
   * @return A string consisting of the string values of the array elements separated by "\n"
   */
    public static String createStringFromArray(Object[] array) {
        StringBuilder sb = new StringBuilder();
        for (Object obj : array) {
            sb.append(obj.toString()).append("\n");
        }
        return sb.toString();
    }

  /**
   * Method that converts a string to an array of Object instances computed by splitting the given string with delimiter "\n"
   * e.g. the string "one\n2\ntrue\n" is converted to the array ["one", "2", "true"]
   *
   * @param str The string to be converted to an array
   * @return An array consisting of string values that occur by splitting the string around "\n"
   */
    public static Object[] createArrayFromString(String str) {
        return str.split("\n");
    }


  /**
   * CArtAgO operation for updating data of a .txt file in a Linked Data Platform container of the Solid pod
   * The method reads the data currently stored in the .txt file and publishes in the file the old data along with new data 
   * 
   * @param containerName The name of the container where the .txt file resource is located
   * @param fileName The name of the .txt file resource that holds the data to be updated
   * @param data An array whose elements are the new data to be added in the .txt file
   */
    @OPERATION
    public void updateData(String containerName, String fileName, Object[] data) {
        Object[] oldData = readData(containerName, fileName);
        Object[] allData = new Object[oldData.length + data.length];
        System.arraycopy(oldData, 0, allData, 0, oldData.length);
        System.arraycopy(data, 0, allData, oldData.length, data.length);
        publishData(containerName, fileName, allData);
    }
}
