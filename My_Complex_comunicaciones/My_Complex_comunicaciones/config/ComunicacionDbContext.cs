using Microsoft.EntityFrameworkCore;
using My_Comunicacion.Models;

namespace My_Comunicacion.Config
{
    public class ComunicacionDbContext : DbContext
    {
        public ComunicacionDbContext(DbContextOptions<ComunicacionDbContext> options)
            : base(options) { }

        public DbSet<Noticia> Noticias { get; set; }
        public DbSet<Reunion> Reuniones { get; set; }
    }
}
