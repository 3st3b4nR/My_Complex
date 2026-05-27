using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using My_Comunicacion.DTOs;
using My_Comunicacion.Services;

[ApiController]
[Route("api/[controller]")]
public class NoticiasController : ControllerBase
{
    private readonly ComunicacionService _service;

    public NoticiasController(ComunicacionService service)
    {
        _service = service;
    }

    [HttpPost("crear")]
    [Authorize(Roles = "Administrador")]
    public IActionResult CrearNoticia([FromBody] NoticiaRequest request)
    {
        _service.CrearNoticia(request);
        return Ok(new { message = "Noticia publicada correctamente" });
    }

    [HttpGet("listar")]
    [AllowAnonymous]
    public IActionResult ListarNoticias()
    {
        var noticias = _service.ListarNoticias();
        return Ok(noticias);
    }
}
