using Microsoft.AspNetCore.Mvc;
using My_Complex.DTOs;
using My_Complex.Services;

namespace My_Complex.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class AuthController : ControllerBase
    {
        private readonly AuthService _auth;

        public AuthController(AuthService auth)
        {
            _auth = auth;
        }

        [HttpPost("login")]
        public IActionResult Login(LoginRequest request)
        {
            var result = _auth.Login(request);

            if (result == null)
                return Unauthorized(new { message = "Credenciales inválidas" });

            return Ok(result);
        }
    }
}
