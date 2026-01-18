package com.printkon.pdp.notification;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.printkon.pdp.notification.config.EmailConfig;
import com.printkon.pdp.user.models.User;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EmailService {

	@Autowired
	private JavaMailSender javaMailSender;

	/**
	 * Generic method to send email
	 */
	public String sendMail(EmailConfig emailConfiguration) {
		try {
			SimpleMailMessage message = new SimpleMailMessage();
			message.setFrom("admin@printkon.com");
			message.setTo(emailConfiguration.getToAddress());
			message.setText(emailConfiguration.getText());
			message.setSubject(emailConfiguration.getSubject());
			javaMailSender.send(message);
			log.info("Email sent successfully to: {}", emailConfiguration.getToAddress());
			return "Email sent successfully";
		} catch (Exception e) {
			log.error("Failed to send email to: {}", emailConfiguration.getToAddress(), e);
			return "Failed to send email: " + e.getMessage();
		}
	}

	/**
	 * Send registration verification email
	 */
	public String sendRegistrationEmail(User user, String activationLink) {
		EmailConfig emailConfig = new EmailConfig();
		emailConfig.setToAddress(user.getEmail());
		emailConfig.setSubject("Welcome! Please Activate Your Account");
		emailConfig.setText(buildRegistrationEmailText(user.getName(), activationLink));

		return sendMail(emailConfig);
	}

	/**
	 * Send password reset email
	 */
	public String sendPasswordResetEmail(User user, String resetLink, long expiryMinutes) {
		EmailConfig emailConfig = new EmailConfig();
		emailConfig.setToAddress(user.getEmail());
		emailConfig.setSubject("Password Reset Request");
		emailConfig.setText(buildPasswordResetEmailText(user.getName(), resetLink, expiryMinutes));

		return sendMail(emailConfig);
	}

	/**
	 * Send password change confirmation email
	 */
	public String sendPasswordChangeConfirmationEmail(User user) {
		EmailConfig emailConfig = new EmailConfig();
		emailConfig.setToAddress(user.getEmail());
		emailConfig.setSubject("Password Changed Successfully");
		emailConfig.setText(buildPasswordChangeConfirmationText(user.getName()));

		return sendMail(emailConfig);
	}

	/**
	 * Send account deletion confirmation email
	 */
	public String sendAccountDeletionEmail(User user) {
		EmailConfig emailConfig = new EmailConfig();
		emailConfig.setToAddress(user.getEmail());
		emailConfig.setSubject("Account Deleted Successfully");
		emailConfig.setText(buildAccountDeletionText(user.getName()));

		return sendMail(emailConfig);
	}

	/**
	 * Send profile update notification email
	 */
	public String sendProfileUpdateNotificationEmail(User user) {
		EmailConfig emailConfig = new EmailConfig();
		emailConfig.setToAddress(user.getEmail());
		emailConfig.setSubject("Profile Updated Successfully");
		emailConfig.setText(buildProfileUpdateNotificationText(user.getName()));

		return sendMail(emailConfig);
	}

	/**
	 * Send role upgrade notification email
	 */
	public String sendRoleUpgradeEmail(User user, String confirmationLink) {
		EmailConfig emailConfig = new EmailConfig();
		emailConfig.setToAddress(user.getEmail());
		emailConfig.setSubject("Role Upgrade Request");
		emailConfig.setText(buildRoleUpgradeEmailText(user.getName(), confirmationLink));

		return sendMail(emailConfig);
	}

	// === EMAIL TEMPLATE METHODS ===

	private String buildRegistrationEmailText(String userName, String activationLink) {
		return String.format("Dear %s,\n\n" + "Welcome to our platform! Thank you for registering with us.\n\n"
				+ "To complete your registration and activate your account, please click on the following link:\n\n"
				+ "%s\n\n" + "This activation link will expire in 15 minutes for security reasons.\n\n"
				+ "If you did not create this account, please ignore this email.\n\n" + "Best regards,\n"
				+ "The Printkon Team\n\n" + "---\n"
				+ "This is an automated email. Please do not reply to this message.", userName, activationLink);
	}

	private String buildPasswordResetEmailText(String userName, String resetLink, long expiryMinutes) {
		return String.format("Dear %s,\n\n" + "We received a request to reset your password for your account.\n\n"
				+ "To reset your password, please click on the following link:\n\n" + "%s\n\n"
				+ "This link will expire in %d minutes for security reasons.\n\n"
				+ "If you did not request a password reset, please ignore this email. Your password will remain unchanged.\n\n"
				+ "For security reasons, please do not share this link with anyone.\n\n" + "Best regards,\n"
				+ "The Printkon Team\n\n" + "---\n"
				+ "This is an automated email. Please do not reply to this message.", userName, resetLink,
				expiryMinutes);
	}

	private String buildPasswordChangeConfirmationText(String userName) {
		return String.format("Dear %s,\n\n" + "Your password has been successfully changed.\n\n"
				+ "If you did not make this change, please contact our support team immediately.\n\n"
				+ "For your security:\n" + "- Always use strong, unique passwords\n"
				+ "- Never share your password with anyone\n"
				+ "- Consider enabling two-factor authentication if available\n\n" + "Best regards,\n"
				+ "The Printkon Team\n\n" + "---\n"
				+ "This is an automated email. Please do not reply to this message.", userName);
	}

	private String buildAccountDeletionText(String userName) {
		return String.format("Dear %s,\n\n" + "Your account has been successfully deleted from our platform.\n\n"
				+ "We're sorry to see you go. If you have any feedback about your experience, "
				+ "we'd love to hear from you.\n\n"
				+ "If this deletion was not requested by you, please contact our support team immediately.\n\n"
				+ "Thank you for being part of our community.\n\n" + "Best regards,\n" + "The Printkon Team\n\n"
				+ "---\n" + "This is an automated email. Please do not reply to this message.", userName);
	}

	private String buildProfileUpdateNotificationText(String userName) {
		return String.format("Dear %s,\n\n" + "Your profile information has been successfully updated.\n\n"
				+ "If you did not make these changes, please secure your account immediately by:\n"
				+ "1. Changing your password\n" + "2. Reviewing your account security settings\n"
				+ "3. Contacting our support team\n\n" + "Best regards,\n" + "The Printkon Team\n\n" + "---\n"
				+ "This is an automated email. Please do not reply to this message.", userName);
	}

	private String buildRoleUpgradeEmailText(String userName, String confirmationLink) {
		return String.format(
				"Dear %s,\n\n" + "We have received a request to upgrade your account role.\n\n"
						+ "To confirm this role upgrade, please click on the following link:\n\n" + "%s\n\n"
						+ "This link will expire in 15 minutes for security reasons.\n\n"
						+ "If you did not request this role upgrade, please ignore this email.\n\n" + "Best regards,\n"
						+ "The Printkon Team\n\n" + "---\n"
						+ "This is an automated email. Please do not reply to this message.",
				userName, confirmationLink);
	}
}