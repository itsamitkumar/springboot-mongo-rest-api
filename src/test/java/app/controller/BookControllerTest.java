package app.controller;

import static com.jayway.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.lessThan;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import app.Application;
import app.model.Book;
import app.repository.BookRepository;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebIntegrationTest
public class BookControllerTest {

	public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Autowired
	private BookRepository bookRepository;

	// Test RestTemplate to invoke the APIs.
	private RestTemplate restTemplate = new TestRestTemplate();

	@Test
	public void canFetchAll() {
		given().when().get("http://localhost:8989/book").then().statusCode(200).and().body("totalBooks", equalTo(0))
				.and().time(lessThan(1000L));
	}

	@Test
	public void testCreateBookApi() throws JsonProcessingException {

		// Building the Request body data
		Map<String, Object> requestBody = new HashMap<String, Object>();
		requestBody.put("name", "Book 1");
		requestBody.put("isbn", "QWER1234");
		requestBody.put("author", "Author 1");
		requestBody.put("pages", 200);
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.setContentType(MediaType.APPLICATION_JSON);

		// Creating http entity object with request body and headers
		HttpEntity<String> httpEntity = new HttpEntity<String>(OBJECT_MAPPER.writeValueAsString(requestBody),
				requestHeaders);

		// Invoking the API
		Map<String, Object> apiResponse = restTemplate.postForObject("http://localhost:8989/book", httpEntity,
				Map.class, Collections.EMPTY_MAP);

		assertNotNull(apiResponse);

		// Asserting the response of the API.
		String message = apiResponse.get("message").toString();
		assertEquals("Book created successfully", message);
		String bookId = ((Map<String, Object>) apiResponse.get("book")).get("id").toString();

		assertNotNull(bookId);

		// Fetching the Book details directly from the DB to verify the API
		// succeeded
		Book bookFromDb = bookRepository.findOne(bookId);
		assertEquals("Book 1", bookFromDb.getName());
		assertEquals("QWER1234", bookFromDb.getIsbn());
		assertEquals("Author 1", bookFromDb.getAuthor());
		assertTrue(200 == bookFromDb.getPages());

		// Delete the data added for testing
		bookRepository.delete(bookId);

	}

	@Test
	public void testGetBookDetailsApi() {
		// Create a new book using the BookRepository API
		Book book = new Book("Book1", "�SBN1", "Author1", 200);
		bookRepository.save(book);

		String bookId = book.getId();

		// Now make a call to the API to get details of the book
		Book apiResponse = restTemplate.getForObject("http://localhost:8989/book/" + bookId, Book.class);

		// Verify that the data from the API and data saved in the DB are same
		assertNotNull(apiResponse);
		assertEquals(book.getName(), apiResponse.getName());
		assertEquals(book.getId(), apiResponse.getId());
		assertEquals(book.getIsbn(), apiResponse.getIsbn());
		assertEquals(book.getAuthor(), apiResponse.getAuthor());
		assertTrue(book.getPages() == apiResponse.getPages());

		// Delete the Test data created
		bookRepository.delete(bookId);
	}

	@Test
	public void testUpdateBookDetails() throws JsonProcessingException {
		// Create a new book using the BookRepository API
		Book book = new Book("Book1", "ISBN1", "Author1", 200);
		bookRepository.save(book);

		String bookId = book.getId();

		// Now create Request body with the updated Book Data.
		Map<String, Object> requestBody = new HashMap<String, Object>();
		requestBody.put("name", "Book2");
		requestBody.put("isbn", "ISBN2");
		requestBody.put("author", "Author2");
		requestBody.put("pages", 200);
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.setContentType(MediaType.APPLICATION_JSON);

		// Creating http entity object with request body and headers
		HttpEntity<String> httpEntity = new HttpEntity<String>(OBJECT_MAPPER.writeValueAsString(requestBody),
				requestHeaders);

		// Invoking the API
		Map<String, Object> apiResponse = (Map) restTemplate.exchange("http://localhost:8989/book/" + bookId,
				HttpMethod.PUT, httpEntity, Map.class, Collections.EMPTY_MAP).getBody();

		assertNotNull(apiResponse);
		assertTrue(!apiResponse.isEmpty());

		// Asserting the response of the API.
		String message = apiResponse.get("message").toString();
		assertEquals("Book Updated successfully", message);

		// Fetching the Book details directly from the DB to verify the API
		// succeeded in updating the book details
		Book bookFromDb = bookRepository.findOne(bookId);
		assertEquals(requestBody.get("name"), bookFromDb.getName());
		assertEquals(requestBody.get("isbn"), bookFromDb.getIsbn());
		assertEquals(requestBody.get("author"), bookFromDb.getAuthor());
		assertTrue(Integer.parseInt(requestBody.get("pages").toString()) == bookFromDb.getPages());

		// Delete the data added for testing
		bookRepository.delete(bookId);

	}

	@Test
	public void testDeleteBookApi() {
		// Create a new book using the BookRepository API
		Book book = new Book("Book1", "ISBN1", "Author1", 200);
		bookRepository.save(book);

		String bookId = book.getId();

		// Now Invoke the API to delete the book
		restTemplate.delete("http://localhost:8989/book/" + bookId, Collections.EMPTY_MAP);

		// Try to fetch from the DB directly
		Book bookFromDb = bookRepository.findOne(bookId);
		// and assert that there is no data found
		assertNull(bookFromDb);
	}

	@Test
	public void testGetAllBooksApi() {
		// Add some test data for the API
		Book book1 = new Book("Book1", "ISBN1", "Author1", 200);
		bookRepository.save(book1);

		Book book2 = new Book("Book2", "ISBN2", "Author2", 200);
		bookRepository.save(book2);

		// Invoke the API
		Map<String, Object> apiResponse = restTemplate.getForObject("http://localhost:8989/book", Map.class);

		// Assert the response from the API
		int totalBooks = Integer.parseInt(apiResponse.get("totalBooks").toString());
		assertTrue(totalBooks == 2);

		List<Map<String, Object>> booksList = (List<Map<String, Object>>) apiResponse.get("books");
		assertTrue(booksList.size() == 2);

		// Delete the test data created
		bookRepository.delete(book1.getId());
		bookRepository.delete(book2.getId());
	}
}
