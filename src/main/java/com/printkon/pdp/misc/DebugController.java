package com.printkon.pdp.misc;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Dev-only debug endpoints for inspecting incoming cookies. Annotated
 * with @Profile("dev") so it only activates when 'dev' profile is active.
 * Remove or disable in production.
 */
@Profile("dev")
@RestController
@RequestMapping
public class DebugController {

	@GetMapping("/cookies")
	public Map<String, String> cookies(HttpServletRequest req) {
		Map<String, String> map = new HashMap<>();
		Cookie[] cookies = req.getCookies();
		if (cookies != null) {
			for (Cookie c : cookies) {
				// Do NOT log sensitive cookie values in production
				map.put(c.getName(), c.getValue());
			}
		}
		return map;
	}
}
