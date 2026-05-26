using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using My_Complex.DTOs;
using My_Complex.Models;
using My_Complex.Services;
using My_Complex.Config;

namespace My_Complex.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class AuthController : ControllerBase
    {
        private readonly AuthService _auth;
        private readonly MyDbContext _context;

        public AuthController(AuthService auth, MyDbContext context)
        {
            _auth = auth;
            _context = context;
        }

        [HttpPost("login")]
        public IActionResult Login(LoginRequest request)
        {
            var result = _auth.Login(request);

            if (result == null)
                return Unauthorized(new { message = "Credenciales inválidas" });

            return Ok(result);
        }

        [HttpPost("register")]
        public IActionResult Register([FromBody] RegisterRequest request)
        {
            // Validar que el rol exista
            var rol = _context.Roles.FirstOrDefault(r => r.Nombre_rol == request.Rol);
            if (rol == null)
                return BadRequest("Rol inválido");

            // Validar que trabajadores y vigilantes NO tengan apartamento
            if ((request.Rol == "Trabajador" || request.Rol == "Vigilante") && request.ApartamentoId != null)
                return BadRequest("Este rol no puede tener apartamentos asociados");

            // Crear usuario
            var usuario = new Usuario
            {
                Nombre = request.Nombre,
                Apellido = request.Apellido,
                Documento = request.Documento,
                Correo = request.Email,
                Clave = request.Password,
                Apartamento_id_apartamento = request.ApartamentoId
            };

            _context.Usuarios.Add(usuario);
            _context.SaveChanges();

            // Relación con rol
            var usuarioRol = new UsuarioRol
            {
                Usuario_id_usuario = usuario.id_usuario,
                Rol_id_rol = rol.Id_rol
            };

            _context.UsuarioRoles.Add(usuarioRol);
            _context.SaveChanges();

            return Ok(new { message = "Usuario registrado correctamente" });
        }

        [HttpPut("update")]
        public IActionResult UpdateUser([FromBody] UpdateUserRequest request)
        {
            var usuario = _context.Usuarios
                .Include(u => u.Telefonos)
                .FirstOrDefault(u => u.id_usuario == request.UsuarioId);

            if (usuario == null)
                return NotFound(new { message = "Usuario no encontrado" });

            // Actualizar contraseña si se envía
            if (!string.IsNullOrEmpty(request.NuevaClave))
                usuario.Clave = request.NuevaClave;

            // Actualizar teléfono si se envía
            if (!string.IsNullOrEmpty(request.NuevoTelefono))
            {
                var telefono = usuario.Telefonos.FirstOrDefault();
                if (telefono != null)
                {
                    telefono.NumeroTel = request.NuevoTelefono;
                }
                else
                {
                    _context.Telefonos.Add(new Telefono
                    {
                        Usuario_id_usuario = usuario.id_usuario,
                        NumeroTel = request.NuevoTelefono,
                        Tipo = "Principal"
                    });
                }
            }

            _context.SaveChanges();

            return Ok(new { message = "Usuario actualizado correctamente" });
        }
    }
}
