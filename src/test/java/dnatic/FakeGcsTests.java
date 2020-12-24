package dnatic;

import com.google.cloud.NoCredentials;
import com.google.cloud.storage.*;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

class FakeGcsTests {
	private final Integer internalPort = 8080;
	private final String storageBucketName = "BUCKET_TEST";
	private Storage storage = null;

	@Rule
	public GenericContainer<?> gcsFakeContainer = new GenericContainer<>(DockerImageName.parse("fsouza/fake-gcs-server:latest"))
			.withCommand("-port", internalPort.toString(), "-scheme", "http")
			.withExposedPorts(internalPort)
			.waitingFor(Wait.forHttp("/").forStatusCode(404));

	@BeforeEach
	private void setUp() {
		gcsFakeContainer.start();
		int mappedPort = gcsFakeContainer.getMappedPort(internalPort);

		StorageOptions storageOps = StorageOptions.newBuilder()
				.setCredentials(NoCredentials.getInstance())
				.setHost("http://" + gcsFakeContainer.getHost() + ":" + mappedPort)
				.setProjectId("IKEA_LOCATION_LOCAL")
				.build();
		storage = storageOps.getService();
	}

	@AfterEach
	private void tearDown() {
		gcsFakeContainer.stop();
	}

	@Test
	public void testCreateBucketWithLibrary() {
		createBucketWithLibrary();
		Assertions.assertTrue(checkBucketExists());
	}

	@Test
	public void testCreateBucketWithCurl() {
		createBucketWithCurl();
		Assertions.assertTrue(checkBucketExists());
	}

	private void createBucketWithCurl() {
		try {
			gcsFakeContainer.execInContainer("apk", "add", "curl");
			gcsFakeContainer.execInContainer("curl", "-X", "POST", "-d", "{ \"name\": \"" + storageBucketName + "\" }", "http://localhost:8080/storage/v1/b");
			System.out.println("Successfully created!");
		} catch (Exception ex) {
			System.err.println("Error creating bucket");
			ex.printStackTrace();
		}
	}

	private void createBucketWithLibrary() {
		try {
			Bucket bucket = storage.create(BucketInfo.of(storageBucketName));
			storage.create(bucket);
		} catch (Exception ex) {
			System.err.println("Error creating bucket");
			ex.printStackTrace();
		}
	}

	private boolean checkBucketExists() {
		return storage.get(storageBucketName, Storage.BucketGetOption.fields()) != null;
	}
}
