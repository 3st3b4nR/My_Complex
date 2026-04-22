using My_Complex.DTOs;
using My_Complex.Models;

namespace My_Complex.Services
{
    public class AuthService
    {
        private readonly IJwtService _jwt;
        private readonly List<User> _users = new()
        {
            new User { Email = "admin@correo.com", PasswordHash = "1234", Role = "Administrador" },
            new User { Email = "residente@correo.com", PasswordHash = "1234", Role = "Residente" }
        };

        public AuthService(IJwtService jwt)
        {
            _jwt = jwt;
        }

        public LoginResponse? Login(LoginRequest request)
        {
            var user = _users.FirstOrDefault(u =>
                u.Email == request.Email &&
                u.PasswordHash == request.Password);

            if (user == null)
                return null;

            var token = _jwt.GenerateToken(user, out var expiresAt);

            return new LoginResponse
            {
                Token = token,
                ExpiresAt = expiresAt
            };
        }



    }
}
