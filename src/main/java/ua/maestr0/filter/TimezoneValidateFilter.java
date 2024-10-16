package ua.maestr0.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpFilter;

import java.io.IOException;

@WebFilter(value = "/time")
public class TimezoneValidateFilter extends HttpFilter {
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        String timezone = servletRequest.getParameter("timezone");
        if (timezone != null) {
            String offsetStr = timezone.replace("UTC", "").trim();
            if(Math.abs(Integer.parseInt(offsetStr)) > 12) {
                servletResponse.getWriter().println("Invalid timezone format. MaxValue = UTC±12.");
                return;
            }
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }
}
