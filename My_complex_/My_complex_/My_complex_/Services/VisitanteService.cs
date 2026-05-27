using My_complex_.Config;
using My_complex_.DTOs;
using My_complex_.Models;

namespace My_complex_.Services
{
	public class VisitanteService
	{
		private readonly VisitantesDbContext _context;

		public VisitanteService(VisitantesDbContext context)
		{
			_context = context;
		}

		public void NotificarVisitante(NotificarVisitanteRequest request)
		{
			var visitante = new Visitante
			{
				Nombre = request.Nombre,
				Apellido = request.Apellido,
				Documento = request.Documento,
				Fecha_Ingreso = DateTime.UtcNow
			};

			_context.Visitantes.Add(visitante);
			_context.SaveChanges();
		}

		public void AutorizarEntrada(AutorizarVisitanteRequest request)
		{
			var visitante = _context.Visitantes.Find(request.VisitanteId);
			if (visitante == null) throw new Exception("Visitante no encontrado");

			var autorizacion = new Autoriza
			{
				Fecha_autorizacion = DateTime.UtcNow,
				Estado = "Autorizado",
				Visitante_id_visitante = request.VisitanteId,
				Usuario_id = request.UsuarioId
			};

			var registro = new RegistroVisita
			{
				Fecha_Ingreso = DateTime.UtcNow,
				Visitante_id_visitante = request.VisitanteId
			};

			_context.Autorizaciones.Add(autorizacion);
			_context.RegistroVisitas.Add(registro);
			_context.SaveChanges();
		}

		public void RegistrarSalida(RegistrarSalidaRequest request)
		{
			var visitante = _context.Visitantes.Find(request.VisitanteId);
			if (visitante == null) throw new Exception("Visitante no encontrado");

			visitante.Fecha_Salida = DateTime.UtcNow;

			var registro = _context.RegistroVisitas.FirstOrDefault(r => r.Visitante_id_visitante == request.VisitanteId);
			if (registro != null)
				registro.Fecha_Salida = DateTime.UtcNow;

			_context.SaveChanges();
		}
	}
}
