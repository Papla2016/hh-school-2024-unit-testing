package ru.hh.school.unittesting.homework;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LibraryManagerTest {
  @Mock
  private NotificationService notificationService;

  @Mock
  private UserService userService;

  @InjectMocks
  private LibraryManager libraryManager;

  @BeforeEach
  void setUp() {
    libraryManager.addBook("book1", 3);
    libraryManager.addBook("book2", 0);
  }

  @Test
  void testCorrectWorkAvailableCopies() {
    assertEquals(3, libraryManager.getAvailableCopies("book1"));
  }

  @Test
  void testCorrectWorkAvailableCopiesIfEmpty() {
    assertEquals(0, libraryManager.getAvailableCopies("book2"));
  }

  @Test
  void testCorrectWorkAvailableCopiesIfNoElement() {
    assertEquals(0, libraryManager.getAvailableCopies("book3"));
  }

  @Test
  void testAddBookIncreaseCorrect() {
    libraryManager.addBook("book1", 2);
    assertEquals(5, libraryManager.getAvailableCopies("book1"));
  }

  @Test
  void testBorrowBookSuccess() {
    when(userService.isUserActive("user")).thenReturn(true);
    boolean result = libraryManager.borrowBook("book1", "user");
    assertTrue(result);
    assertEquals(2, libraryManager.getAvailableCopies("book1"));
    verify(notificationService).notifyUser("user", "You have borrowed the book: book1");
  }

  @Test
  void testBorrowBookFailForInactiveUser() {
    when(userService.isUserActive("user")).thenReturn(false);
    boolean result = libraryManager.borrowBook("book1", "user");
    assertFalse(result);
    verify(notificationService).notifyUser("user", "Your account is not active.");
    assertEquals(3, libraryManager.getAvailableCopies("book1"));
  }

  @Test
  void testBorrowBookFailWhenOutOfStock() {
    when(userService.isUserActive("user")).thenReturn(true);
    boolean result = libraryManager.borrowBook("book2", "user");
    assertFalse(result);
    assertEquals(0, libraryManager.getAvailableCopies("book2"));
    verify(notificationService, never()).notifyUser(eq("user"), anyString());
  }

  @Test
  void testReturnBookSuccsess() {
    when(userService.isUserActive("user")).thenReturn(true);
    libraryManager.borrowBook("book1", "user");
    assertTrue(libraryManager.returnBook("book1", "user"));
    assertEquals(3, libraryManager.getAvailableCopies("book1"));
    verify(notificationService).notifyUser("user", "You have returned the book: book1");
  }

  @Test
  void testReturnBookFailsIfNotBorrowed() {

    boolean result = libraryManager.returnBook("book1", "user");
    assertFalse(result);
    assertEquals(3, libraryManager.getAvailableCopies("book1"));
    verify(notificationService, never()).notifyUser(eq("user"), anyString());
  }

  @Test
  void testReturnBookFailsIfBorrowBookAndWrongUser() {
    when(userService.isUserActive("user")).thenReturn(true);
    libraryManager.borrowBook("book1", "user");
    boolean result = libraryManager.returnBook("book1", "user1");
    assertFalse(result);
    assertEquals(2, libraryManager.getAvailableCopies("book1"));
    verify(notificationService, never()).notifyUser(eq("user1"), anyString());
  }

  @ParameterizedTest
  @CsvSource({
      "0, false, false, 0.00",
      "1, false, false, 0.50",
      "1, true, false, 0.75",
      "1, false, true, 0.40",
      "1, true, true, 0.60",
      "10, true, false, 7.50",
      "10, true, true, 6.00"
  })
  void testCalculateDynamicLAteFee(
      int overdueDays,
      boolean isBestseller,
      boolean isPremiumMember,
      double expectedFee
  ) {
    double fee = libraryManager.calculateDynamicLateFee(overdueDays, isBestseller, isPremiumMember);
    assertEquals(expectedFee, fee);
  }

  @Test
  void testCalculateDynamicLateFeeThrowsExceptionForNegativeDays() {
    assertThrows(IllegalArgumentException.class, () ->
        libraryManager.calculateDynamicLateFee(-1, false, false)
    );
  }
}
