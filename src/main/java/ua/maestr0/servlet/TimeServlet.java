package ua.maestr0.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@WebServlet(value = "/time")
public class TimeServlet extends HttpServlet {
    private static final String TIME_ZONE_PARAMETER_NAME = "timezone";
    private transient TemplateEngine templateEngine;

    @Override
    public void init() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setPrefix("/templates/");
        resolver.setSuffix(".html");

        templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(resolver);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        resp.setContentType("text/html");
        resp.setCharacterEncoding("UTF-8");

        ZonedDateTime now = getZonedDateTime(req, resp);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String offsetDisplay = now.getZone().getId();
        String formattedTime = now.format(formatter);

        printResponse(resp, formattedTime, offsetDisplay);
    }

    private void printResponse(HttpServletResponse resp, String formattedTime, String offsetDisplay) {
        Context context = new Context();
        context.setVariable("date", formattedTime + " " + offsetDisplay);

        try {
            templateEngine.process("index", context, resp.getWriter());
            resp.setStatus(HttpServletResponse.SC_OK);
        } catch (IOException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace();
        }
    }

    private void printError(HttpServletResponse resp) {
        try {
            resp.setContentType("text/plain");
            resp.getWriter().println("Invalid timezone format. Use 'UTC', 'UTC+2', 'UTC-2', or a valid timezone ID.");
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        } catch (IOException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            throw new RuntimeException();
        }
    }

    private ZonedDateTime getZonedDateTime(HttpServletRequest req, HttpServletResponse resp) {
        ZoneId zoneId = getZoneIdFromParameter(req.getParameter(TIME_ZONE_PARAMETER_NAME), resp);
        if (zoneId != null) {
            saveTimezoneCookie(resp, zoneId);
        } else {
            zoneId = getZoneIdFromCookies(req);
            if (zoneId == null) {
                zoneId = ZoneId.of("UTC");
            }
        }
        return ZonedDateTime.now(zoneId);
    }

    private ZoneId getZoneIdFromParameter(String timezoneParam, HttpServletResponse resp) {
        if (timezoneParam != null && !timezoneParam.isEmpty()) {
            ZoneId zoneId = parseUtcOffset(timezoneParam);
            if(zoneId != null) {
                return zoneId;
            } else {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                printError(resp);
            }
        }
        return null;
    }

    private ZoneId parseUtcOffset(String timezoneParam) {
        String offsetStr = timezoneParam.substring(3).trim();
        if (Integer.parseInt(offsetStr) >= 0) {
            offsetStr = "+" + offsetStr;
        }
        return ZoneId.of("UTC" + offsetStr);
    }

    private void saveTimezoneCookie(HttpServletResponse resp, ZoneId zoneId) {
        Cookie timezoneCookie = new Cookie(TIME_ZONE_PARAMETER_NAME, zoneId.getId());
        timezoneCookie.setMaxAge(20);
        resp.addCookie(timezoneCookie);
    }

    private ZoneId getZoneIdFromCookies(HttpServletRequest req) {
        if (req.getCookies() != null) {
            for (Cookie cookie : req.getCookies()) {
                if (TIME_ZONE_PARAMETER_NAME.equals(cookie.getName())) {
                    return ZoneId.of(cookie.getValue());
                }
            }
        }
        return null;
    }
}
