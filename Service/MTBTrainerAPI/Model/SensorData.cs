using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace MTBTrainerAPI
{
    [Table("SensorData")]
    public class SensorData
    {
        [Key]
        [Required]
        public int id { get; set; }

        [StringLength(50)]
        public string horizontalAccel { get; set; }
    }
}