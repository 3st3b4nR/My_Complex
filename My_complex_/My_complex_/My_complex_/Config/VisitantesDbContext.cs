using Microsoft.EntityFrameworkCore;
using My_complex_.Models; // Ajusta el namespace según tu proyecto

namespace My_complex_.Config
{
	public class VisitantesDbContext : DbContext
	{
		public VisitantesDbContext(DbContextOptions<VisitantesDbContext> options)
			: base(options)
		{
		}

		public DbSet<Visitante> Visitantes { get; set; }
		public DbSet<RegistroVisita> RegistroVisitas { get; set; }
		public DbSet<Autoriza> Autorizaciones { get; set; }

		protected override void OnModelCreating(ModelBuilder modelBuilder)
		{
			modelBuilder.Entity<Visitante>().HasKey(v => v.id_visitante);
			modelBuilder.Entity<RegistroVisita>().HasKey(r => r.id_registro);
			modelBuilder.Entity<Autoriza>().HasKey(a => a.id_autorizacion);

			modelBuilder.Entity<RegistroVisita>()
				.HasOne<Visitante>()
				.WithMany()
				.HasForeignKey(r => r.Visitante_id_visitante);

			modelBuilder.Entity<Autoriza>()
				.HasOne<Visitante>()
				.WithMany()
				.HasForeignKey(a => a.Visitante_id_visitante);
		}
	}
}
