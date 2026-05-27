using Microsoft.AspNetCore.Mvc;
using My_complex_.Config;
using My_complex_.DTOs;
using My_complex_.Models;
using My_complex_.Services;

namespace My_complex_.Controllers
{
	[ApiController]
	[Route("api/[controller]")]
	public class VisitantesController : ControllerBase
	{
		private readonly VisitanteService _service;

		public VisitantesController(VisitanteService service)
		{
			_service = service;
		}

		[HttpPost("notificar")]
		public IActionResult NotificarVisitante([FromBody] NotificarVisitanteRequest request)
		{
			_service.NotificarVisitante(request);
			return Ok(new { message = "Notificación enviada al residente/propietario" });
		}

		[HttpPost("autorizar")]
		public IActionResult AutorizarEntrada([FromBody] AutorizarVisitanteRequest request)
		{
			_service.AutorizarEntrada(request);
			return Ok(new { message = "Entrada autorizada y registrada" });
		}

		[HttpPut("salida")]
		public IActionResult RegistrarSalida([FromBody] RegistrarSalidaRequest request)
		{
			_service.RegistrarSalida(request);
			return Ok(new { message = "Salida registrada correctamente" });
		}
	}
}
