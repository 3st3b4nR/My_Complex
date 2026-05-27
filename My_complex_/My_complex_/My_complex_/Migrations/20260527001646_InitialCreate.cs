using System;
using Microsoft.EntityFrameworkCore.Migrations;
using Npgsql.EntityFrameworkCore.PostgreSQL.Metadata;

#nullable disable

namespace My_complex_.Migrations
{
    /// <inheritdoc />
    public partial class InitialCreate : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.CreateTable(
                name: "Visitantes",
                columns: table => new
                {
                    id_visitante = table.Column<int>(type: "integer", nullable: false)
                        .Annotation("Npgsql:ValueGenerationStrategy", NpgsqlValueGenerationStrategy.IdentityByDefaultColumn),
                    Nombre = table.Column<string>(type: "text", nullable: false),
                    Apellido = table.Column<string>(type: "text", nullable: false),
                    Documento = table.Column<string>(type: "text", nullable: false),
                    Fecha_Ingreso = table.Column<DateTime>(type: "timestamp with time zone", nullable: false),
                    Fecha_Salida = table.Column<DateTime>(type: "timestamp with time zone", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_Visitantes", x => x.id_visitante);
                });

            migrationBuilder.CreateTable(
                name: "Autorizaciones",
                columns: table => new
                {
                    id_autorizacion = table.Column<int>(type: "integer", nullable: false)
                        .Annotation("Npgsql:ValueGenerationStrategy", NpgsqlValueGenerationStrategy.IdentityByDefaultColumn),
                    Fecha_autorizacion = table.Column<DateTime>(type: "timestamp with time zone", nullable: false),
                    Estado = table.Column<string>(type: "text", nullable: false),
                    Visitante_id_visitante = table.Column<int>(type: "integer", nullable: false),
                    Usuario_id = table.Column<int>(type: "integer", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_Autorizaciones", x => x.id_autorizacion);
                    table.ForeignKey(
                        name: "FK_Autorizaciones_Visitantes_Visitante_id_visitante",
                        column: x => x.Visitante_id_visitante,
                        principalTable: "Visitantes",
                        principalColumn: "id_visitante",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateTable(
                name: "RegistroVisitas",
                columns: table => new
                {
                    id_registro = table.Column<int>(type: "integer", nullable: false)
                        .Annotation("Npgsql:ValueGenerationStrategy", NpgsqlValueGenerationStrategy.IdentityByDefaultColumn),
                    Fecha_Ingreso = table.Column<DateTime>(type: "timestamp with time zone", nullable: false),
                    Fecha_Salida = table.Column<DateTime>(type: "timestamp with time zone", nullable: true),
                    Visitante_id_visitante = table.Column<int>(type: "integer", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_RegistroVisitas", x => x.id_registro);
                    table.ForeignKey(
                        name: "FK_RegistroVisitas_Visitantes_Visitante_id_visitante",
                        column: x => x.Visitante_id_visitante,
                        principalTable: "Visitantes",
                        principalColumn: "id_visitante",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateIndex(
                name: "IX_Autorizaciones_Visitante_id_visitante",
                table: "Autorizaciones",
                column: "Visitante_id_visitante");

            migrationBuilder.CreateIndex(
                name: "IX_RegistroVisitas_Visitante_id_visitante",
                table: "RegistroVisitas",
                column: "Visitante_id_visitante");
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(
                name: "Autorizaciones");

            migrationBuilder.DropTable(
                name: "RegistroVisitas");

            migrationBuilder.DropTable(
                name: "Visitantes");
        }
    }
}
