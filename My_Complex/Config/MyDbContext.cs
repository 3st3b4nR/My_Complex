using Microsoft.EntityFrameworkCore;
using My_Complex.Models;

namespace My_Complex.Config
{
    public class MyDbContext : DbContext
    {
        public MyDbContext(DbContextOptions<MyDbContext> options) : base(options) { }

        public DbSet<Usuario> Usuarios { get; set; }
        public DbSet<Rol> Roles { get; set; }
        public DbSet<UsuarioRol> UsuarioRoles { get; set; }
        public DbSet<Apartamento> Apartamentos { get; set; }
        public DbSet<Telefono> Telefonos { get; set; }

        protected override void OnModelCreating(ModelBuilder modelBuilder)
        {
            modelBuilder.Entity<UsuarioRol>()
                .HasKey(ur => new { ur.Rol_id_rol, ur.Usuario_id_usuario });

            modelBuilder.Entity<UsuarioRol>()
                .HasOne(ur => ur.Usuario)
                .WithMany(u => u.UsuarioRoles)
                .HasForeignKey(ur => ur.Usuario_id_usuario);

            modelBuilder.Entity<UsuarioRol>()
                .HasOne(ur => ur.Rol)
                .WithMany(r => r.UsuarioRoles)
                .HasForeignKey(ur => ur.Rol_id_rol);

            modelBuilder.Entity<Apartamento>()
                .HasOne(a => a.Usuario)
                .WithMany(u => u.Apartamentos)
                .HasForeignKey(a => a.Usuario_id_usuario);

            modelBuilder.Entity<Telefono>()
                .HasOne(t => t.Usuario)
                .WithMany(u => u.Telefonos)
                .HasForeignKey(t => t.Usuario_id_usuario);
        }
    }
}
