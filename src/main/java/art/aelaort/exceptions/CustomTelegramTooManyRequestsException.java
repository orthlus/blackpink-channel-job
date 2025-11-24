package art.aelaort.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

@Getter
@AllArgsConstructor
public class CustomTelegramTooManyRequestsException extends RuntimeException {
	private TelegramApiRequestException ex;
}
