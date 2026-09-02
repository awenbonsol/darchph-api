package ph.darch.api.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTest.TestController.class})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void validationFailureReturns400WithDetailsMap() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.details.name").exists())
                .andExpect(jsonPath("$.path").value("/test/validate"));
    }

    @Test
    void notFoundExceptionReturns404ErrorShape() throws Exception {
        mockMvc.perform(post("/test/notfound"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("missing"))
                .andExpect(jsonPath("$.path").value("/test/notfound"));
    }

    @RestController
    static class TestController {

        @PostMapping("/test/validate")
        public ResponseEntity<Void> validate(@Valid @RequestBody RequestBodyDto body) {
            return ResponseEntity.ok().build();
        }

        @PostMapping("/test/notfound")
        public ResponseEntity<Void> notFound() {
            throw new NotFoundException("missing");
        }
    }

    static class RequestBodyDto {
        @NotBlank(message = "name is required")
        private String name;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
