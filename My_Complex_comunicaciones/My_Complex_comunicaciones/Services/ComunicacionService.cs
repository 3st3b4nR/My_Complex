using Microsoft.Extensions.Caching.Memory;
using My_Comunicacion.Config;
using My_Comunicacion.Models;
using My_Comunicacion.DTOs;

namespace My_Comunicacion.Services
{
    public class ComunicacionService
    {
        private readonly ComunicacionDbContext _context;
        private readonly IMemoryCache _cache;

        public ComunicacionService(ComunicacionDbContext context, IMemoryCache cache)
        {
            _context = context;
            _cache = cache;
        }

        public void CrearNoticia(NoticiaRequest request)
        {
            var noticia = new Noticia
            {
                Titulo = request.Titulo,
                Contenido = request.Contenido,
                Fecha = DateTime.Now,
                Usuario_id = request.Usuario_id
            };
            _context.Noticias.Add(noticia);
            _context.SaveChanges();
            _cache.Remove("NoticiasCache"); // limpiar caché
        }

        public IEnumerable<Noticia> ListarNoticias()
        {
            if (!_cache.TryGetValue("NoticiasCache", out List<Noticia> noticias))
            {
                noticias = _context.Noticias.ToList();
                _cache.Set("NoticiasCache", noticias, TimeSpan.FromMinutes(5));
            }
            return noticias;
        }

        public void CrearReunion(ReunionRequest request)
        {
            var reunion = new Reunion
            {
                Titulo = request.Titulo,
                Descripcion = request.Descripcion,
                Fecha = request.Fecha,
                Lugar = request.Lugar,
                Hora = request.Hora,
                Usuario_id = request.Usuario_id
            };
            _context.Reuniones.Add(reunion);
            _context.SaveChanges();
        }

        public IEnumerable<Reunion> ListarReuniones()
        {
            return _context.Reuniones.ToList();
        }
    }
}
