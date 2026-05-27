using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using My_Comunicacion.DTOs;
using My_Comunicacion.Services;

[ApiController]
[Route("api/[controller]")]
public class ReunionesController : ControllerBase
{
    private readonly ComunicacionService _service;

    public ReunionesController(ComunicacionService service)
    {
        _service = service;
    }

    [HttpPost("crear")]
    [Authorize(Roles = "Administrador")]
    public IActionResult CrearReunion([FromBody] ReunionRequest request)
    {
        _service.CrearReunion(request);
        return Ok(new { message = "Reunión creada correctamente" });
    }

    [HttpGet("listar")]
    [Authorize(Roles = "Propietario")]
    public IActionResult ListarReuniones()
    {
        var reuniones = _service.ListarReuniones();
        return Ok(reuniones);
    }
}
