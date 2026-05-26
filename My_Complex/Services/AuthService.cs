using My_Complex.DTOs;
using My_Complex.Models;
using My_Complex.Config;
using Microsoft.EntityFrameworkCore;

namespace My_Complex.Services
{
    public class AuthService
    {
        private readonly IJwtService _jwt;
        private readonly MyDbContext _context;

        public AuthService(IJwtService jwt, MyDbContext context)
        {
            _jwt = jwt;
            _context = context;
        }

        public LoginResponse? Login(LoginRequest request)
        {
            // Buscar usuario en la base de datos
            var usuario = _context.Usuarios
                .Include(u => u.UsuarioRoles)
                .ThenInclude(ur => ur.Rol)
                .FirstOrDefault(u => u.Correo == request.Email && u.Clave == request.Password);

            if (usuario == null)
                return null;

            // Obtener el rol principal del usuario
            var rol = usuario.UsuarioRoles.FirstOrDefault()?.Rol.Nombre_rol ?? "SinRol";

            // Generar el token JWT
            var token = _jwt.GenerateToken(usuario.Correo, rol, out var expiresAt);

            return new LoginResponse
            {
                Token = token,
                ExpiresAt = expiresAt
            };
        }
    }
}
