using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace My_Complex.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class ApartamentosController : ControllerBase
    {
        // Endpoint protegido: solo Propietarios y Residentes
        [Authorize(Roles = "Propietario,Residente")]
        [HttpGet("cantidad")]
        public IActionResult GetCantidadApartamentos()
        {
            // Valores simulados
            var cantidad = new Random().Next(1, 5); // entre 1 y 4 apartamentos

            return Ok(new
            {
                message = "Acceso permitido",
                user = User.Identity?.Name,
                role = User.FindFirst("role")?.Value,
                cantidadApartamentos = cantidad
            });
        }
    }
}
